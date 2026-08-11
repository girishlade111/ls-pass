package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.crypto.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GeneratorType {
    PASSWORD,
    PASSPHRASE
}

class GeneratorViewModel : ViewModel() {

    private val _generatorType = MutableStateFlow(GeneratorType.PASSWORD)
    val generatorType: StateFlow<GeneratorType> = _generatorType.asStateFlow()

    // Password Options
    private val _length = MutableStateFlow(16)
    val length: StateFlow<Int> = _length.asStateFlow()

    private val _includeUpper = MutableStateFlow(true)
    val includeUpper: StateFlow<Boolean> = _includeUpper.asStateFlow()

    private val _includeLower = MutableStateFlow(true)
    val includeLower: StateFlow<Boolean> = _includeLower.asStateFlow()

    private val _includeNumbers = MutableStateFlow(true)
    val includeNumbers: StateFlow<Boolean> = _includeNumbers.asStateFlow()

    private val _includeSpecial = MutableStateFlow(true)
    val includeSpecial: StateFlow<Boolean> = _includeSpecial.asStateFlow()

    private val _minNumbers = MutableStateFlow(1)
    val minNumbers: StateFlow<Int> = _minNumbers.asStateFlow()

    private val _minSpecial = MutableStateFlow(1)
    val minSpecial: StateFlow<Int> = _minSpecial.asStateFlow()

    private val _avoidAmbiguous = MutableStateFlow(false)
    val avoidAmbiguous: StateFlow<Boolean> = _avoidAmbiguous.asStateFlow()

    // Passphrase Options
    private val _wordCount = MutableStateFlow(4)
    val wordCount: StateFlow<Int> = _wordCount.asStateFlow()

    private val _separator = MutableStateFlow("-")
    val separator: StateFlow<String> = _separator.asStateFlow()

    private val _capitalize = MutableStateFlow(true)
    val capitalize: StateFlow<Boolean> = _capitalize.asStateFlow()

    private val _includeNumberInPassphrase = MutableStateFlow(true)
    val includeNumberInPassphrase: StateFlow<Boolean> = _includeNumberInPassphrase.asStateFlow()

    private val _generatedSecret = MutableStateFlow("")
    val generatedSecret: StateFlow<String> = _generatedSecret.asStateFlow()

    init {
        regenerate()
    }

    fun setGeneratorType(type: GeneratorType) {
        _generatorType.value = type
        regenerate()
    }

    fun setLength(value: Int) {
        _length.value = value.coerceIn(5, 128)
        regenerate()
    }

    fun setIncludeUpper(value: Boolean) {
        _includeUpper.value = value
        regenerate()
    }

    fun setIncludeLower(value: Boolean) {
        _includeLower.value = value
        regenerate()
    }

    fun setIncludeNumbers(value: Boolean) {
        _includeNumbers.value = value
        regenerate()
    }

    fun setIncludeSpecial(value: Boolean) {
        _includeSpecial.value = value
        regenerate()
    }

    fun setMinNumbers(value: Int) {
        _minNumbers.value = value.coerceIn(0, 10)
        regenerate()
    }

    fun setMinSpecial(value: Int) {
        _minSpecial.value = value.coerceIn(0, 10)
        regenerate()
    }

    fun setAvoidAmbiguous(value: Boolean) {
        _avoidAmbiguous.value = value
        regenerate()
    }

    fun setWordCount(value: Int) {
        _wordCount.value = value.coerceIn(3, 20)
        regenerate()
    }

    fun setSeparator(value: String) {
        _separator.value = value
        regenerate()
    }

    fun setCapitalize(value: Boolean) {
        _capitalize.value = value
        regenerate()
    }

    fun setIncludeNumberInPassphrase(value: Boolean) {
        _includeNumberInPassphrase.value = value
        regenerate()
    }

    fun regenerate() {
        if (_generatorType.value == GeneratorType.PASSWORD) {
            _generatedSecret.value = CryptoManager.generatePassword(
                length = _length.value,
                includeUpper = _includeUpper.value,
                includeLower = _includeLower.value,
                includeNumbers = _includeNumbers.value,
                includeSpecial = _includeSpecial.value,
                minNumbers = _minNumbers.value,
                minSpecial = _minSpecial.value,
                avoidAmbiguous = _avoidAmbiguous.value
            )
        } else {
            _generatedSecret.value = CryptoManager.generatePassphrase(
                wordCount = _wordCount.value,
                separator = _separator.value,
                capitalize = _capitalize.value,
                includeNumber = _includeNumberInPassphrase.value
            )
        }
    }
}
