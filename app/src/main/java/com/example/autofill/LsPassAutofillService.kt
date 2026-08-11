package com.example.autofill

import android.app.PendingIntent
import android.content.Intent
import android.app.assist.AssistStructure
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.data.db.LsPassDatabase
import com.example.data.models.DecryptedVaultItem
import com.example.data.models.ItemType
import com.example.data.repository.VaultRepository
import com.example.session.VaultSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@RequiresApi(Build.VERSION_CODES.O)
class LsPassAutofillService : AutofillService() {

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        if (cancellationSignal.isCanceled) return

        val contexts = request.fillContexts
        val structure = contexts.lastOrNull()?.structure ?: return callback.onSuccess(null)

        val targetPackageName = structure.activityComponent?.packageName
        val webDomain = extractWebDomain(structure)
        val cleanDomain = webDomain?.let { cleanDomainHost(it) } ?: ""
        val cleanPackage = targetPackageName?.let { cleanDomainHost(it) } ?: ""

        val usernameFields = mutableListOf<AutofillId>()
        val passwordFields = mutableListOf<AutofillId>()

        parseStructure(structure, usernameFields, passwordFields)

        if (usernameFields.isEmpty() && passwordFields.isEmpty()) {
            return callback.onSuccess(null)
        }

        val masterKey = VaultSessionManager.getSharedMasterKey()
            ?: VaultSessionManager(applicationContext).getActiveMasterKey()

        val fillResponseBuilder = FillResponse.Builder()

        if (masterKey == null) {
            // Vault is locked: Provide a dataset prompt to open & unlock LS Pass
            val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                openIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
                setTextViewText(android.R.id.text1, "LS Pass (Locked)")
                setTextViewText(android.R.id.text2, "Tap to open vault and unlock credentials")
            }

            val datasetBuilder = Dataset.Builder(presentation)
            var hasField = false

            usernameFields.firstOrNull()?.let { id ->
                datasetBuilder.setValue(id, AutofillValue.forText(""), presentation)
                hasField = true
            }
            passwordFields.firstOrNull()?.let { id ->
                datasetBuilder.setValue(id, AutofillValue.forText(""), presentation)
                hasField = true
            }

            if (hasField) {
                datasetBuilder.setAuthentication(pendingIntent.intentSender)
                fillResponseBuilder.addDataset(datasetBuilder.build())
                return callback.onSuccess(fillResponseBuilder.build())
            } else {
                return callback.onSuccess(null)
            }
        }

        // Vault is UNLOCKED: Read credentials from local Room Database (Zero Network)
        val decryptedItems: List<DecryptedVaultItem> = try {
            val db = LsPassDatabase.getInstance(applicationContext)
            val repo = VaultRepository(db.vaultDao())
            runBlocking(Dispatchers.IO) {
                repo.getDecryptedItems(masterKey).first()
            }
        } catch (_: Exception) {
            emptyList()
        }

        val loginItems = decryptedItems.filter { it.type == ItemType.LOGIN || it.loginData != null }

        // Rank items: direct domain or package match first, then title match, then remaining
        val matchingItems = loginItems.sortedByDescending { item ->
            var score = 0
            val itemUris = item.loginData?.uris?.map { cleanDomainHost(it) } ?: emptyList()
            val itemNameLower = item.name.lowercase()

            if (cleanDomain.isNotEmpty() && itemUris.any { it.contains(cleanDomain) || cleanDomain.contains(it) }) {
                score += 100
            }
            if (cleanPackage.isNotEmpty() && itemUris.any { it.contains(cleanPackage) || cleanPackage.contains(it) }) {
                score += 80
            }
            if (cleanDomain.isNotEmpty() && (itemNameLower.contains(cleanDomain) || cleanDomain.contains(itemNameLower))) {
                score += 50
            }
            if (cleanPackage.isNotEmpty() && itemNameLower.contains(cleanPackage)) {
                score += 30
            }
            score
        }

        var datasetCount = 0
        for (item in matchingItems) {
            val username = item.loginData?.username ?: ""
            val password = item.loginData?.password ?: ""

            if (username.isBlank() && password.isBlank()) continue

            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
                setTextViewText(android.R.id.text1, item.name)
                setTextViewText(
                    android.R.id.text2,
                    if (username.isNotBlank()) username else "••••••••"
                )
            }

            val datasetBuilder = Dataset.Builder()
            var addedToDataset = false

            for (uId in usernameFields) {
                datasetBuilder.setValue(uId, AutofillValue.forText(username), presentation)
                addedToDataset = true
            }

            for (pId in passwordFields) {
                datasetBuilder.setValue(pId, AutofillValue.forText(password), presentation)
                addedToDataset = true
            }

            if (addedToDataset) {
                fillResponseBuilder.addDataset(datasetBuilder.build())
                datasetCount++
            }

            if (datasetCount >= 5) break
        }

        // Configure SaveInfo if user enters new password
        val allAllIds = (usernameFields + passwordFields).toTypedArray()
        if (allAllIds.isNotEmpty()) {
            val saveInfoType = SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME
            val saveInfo = SaveInfo.Builder(saveInfoType, allAllIds).build()
            fillResponseBuilder.setSaveInfo(saveInfo)
        }

        if (datasetCount > 0) {
            callback.onSuccess(fillResponseBuilder.build())
        } else {
            callback.onSuccess(null)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Zero network requirement, handles local save acknowledgment
        callback.onSuccess()
    }

    private fun extractWebDomain(structure: AssistStructure): String? {
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            val domain = findWebDomain(windowNode.rootViewNode)
            if (!domain.isNullOrBlank()) return domain
        }
        return null
    }

    private fun findWebDomain(node: AssistStructure.ViewNode): String? {
        if (!node.webDomain.isNullOrBlank()) {
            return node.webDomain
        }
        for (i in 0 until node.childCount) {
            val domain = findWebDomain(node.getChildAt(i))
            if (!domain.isNullOrBlank()) return domain
        }
        return null
    }

    private fun cleanDomainHost(rawUrlOrDomain: String): String {
        var s = rawUrlOrDomain.trim().lowercase()
        if (s.startsWith("https://")) s = s.removePrefix("https://")
        if (s.startsWith("http://")) s = s.removePrefix("http://")
        if (s.startsWith("www.")) s = s.removePrefix("www.")
        val firstSlash = s.indexOf('/')
        if (firstSlash != -1) s = s.substring(0, firstSlash)
        val firstColon = s.indexOf(':')
        if (firstColon != -1) s = s.substring(0, firstColon)
        return s
    }

    private fun parseStructure(
        structure: AssistStructure,
        usernameFields: MutableList<AutofillId>,
        passwordFields: MutableList<AutofillId>
    ) {
        val windowNodesCount = structure.windowNodeCount
        for (i in 0 until windowNodesCount) {
            val windowNode = structure.getWindowNodeAt(i)
            val rootNode = windowNode.rootViewNode
            traverseNode(rootNode, usernameFields, passwordFields)
        }
    }

    private fun traverseNode(
        node: AssistStructure.ViewNode,
        usernameFields: MutableList<AutofillId>,
        passwordFields: MutableList<AutofillId>
    ) {
        val autofillId = node.autofillId
        if (autofillId != null) {
            val hints = node.autofillHints
            val idEntry = node.idEntry?.lowercase() ?: ""
            val hintText = node.hint?.lowercase() ?: ""

            var isUsername = false
            var isPassword = false

            if (!hints.isNullOrEmpty()) {
                for (hint in hints) {
                    val lower = hint.lowercase()
                    if (lower.contains("username") || lower.contains("email") || lower == View.AUTOFILL_HINT_USERNAME || lower == View.AUTOFILL_HINT_EMAIL_ADDRESS) {
                        isUsername = true
                    } else if (lower.contains("password") || lower == View.AUTOFILL_HINT_PASSWORD) {
                        isPassword = true
                    }
                }
            }

            val htmlInfo = node.htmlInfo
            if (htmlInfo != null) {
                val typeAttr = htmlInfo.attributes?.find { it.first.equals("type", ignoreCase = true) }?.second?.lowercase() ?: ""
                val nameAttr = htmlInfo.attributes?.find { it.first.equals("name", ignoreCase = true) }?.second?.lowercase() ?: ""
                if (typeAttr == "password" || nameAttr.contains("pass")) {
                    isPassword = true
                } else if (typeAttr == "email" || typeAttr == "text" || nameAttr.contains("user") || nameAttr.contains("email") || nameAttr.contains("login")) {
                    isUsername = true
                }
            }

            val inputType = node.inputType
            val isPasswordInput = (inputType and android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0 ||
                    (inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                    (inputType and android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0 ||
                    (inputType and android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0

            val isEmailInput = (inputType and android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0 ||
                    (inputType and android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) != 0

            if (isPasswordInput) {
                isPassword = true
            } else if (isEmailInput) {
                isUsername = true
            }

            if (!isUsername && !isPassword) {
                if (idEntry.contains("pass") || idEntry.contains("pwd") || hintText.contains("password")) {
                    isPassword = true
                } else if (idEntry.contains("user") || idEntry.contains("email") || idEntry.contains("login") ||
                    hintText.contains("email") || hintText.contains("username") || hintText.contains("user")) {
                    isUsername = true
                }
            }

            if (isPassword) {
                if (!passwordFields.contains(autofillId)) passwordFields.add(autofillId)
            } else if (isUsername) {
                if (!usernameFields.contains(autofillId)) usernameFields.add(autofillId)
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i), usernameFields, passwordFields)
        }
    }
}
