package com.dev.bernardoslailati.conversordemoedas

import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dev.bernardoslailati.conversordemoedas.databinding.ActivityMainBinding
import com.dev.bernardoslailati.conversordemoedas.network.model.CurrencyType
import com.dev.bernardoslailati.conversordemoedas.ui.CurrencyTypesAdapter
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel by viewModels<CurrencyExchangeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        viewModel.requireCurrencyTypes()
        binding.etFromExchangeValue.addCurrencyMask()

        lifecycleScope.apply {
            launch {
                viewModel.currencyTypes.collect { result ->
                    result.onSuccess { currencyTypes ->
                        binding.configureCurrencyTypes(currencyTypes = currencyTypes)
                    }.onFailure {
                        Toast.makeText(
                            this@MainActivity, it.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            launch {
                viewModel.exchangeRate.collect { result ->
                    result.onSuccess {
                        Log.d("MainActivity", it.toString())
                    }.onFailure {
                        Log.d("MainActivity", it.message.toString())
                    }
                }
            }
        }
    }

    private fun ActivityMainBinding.configureCurrencyTypes(currencyTypes: List<CurrencyType>) {
        spnFromExchange.apply {
            adapter = CurrencyTypesAdapter(currencyTypes)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val from = currencyTypes[position]
                    val to = currencyTypes[spnToExchange.selectedItemPosition]

                    tvFromCurrencySymbol.text = from.symbol
                    viewModel.requireExchangeRate(
                        from = from.acronym,
                        to = to.acronym
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}

            }
        }


        spnToExchange.apply {
            adapter = CurrencyTypesAdapter(currencyTypes)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val from = currencyTypes[spnFromExchange.selectedItemPosition]
                    val to = currencyTypes[position]

                    tvToCurrencySymbol.text = to.symbol
                    viewModel.requireExchangeRate(
                        from = from.acronym,
                        to = to.acronym
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    currencyTypes.firstOrNull()?.let { firstCurrencyType ->
                        tvFromCurrencySymbol.text = firstCurrencyType.symbol
                        tvToCurrencySymbol.text = firstCurrencyType.symbol
                        viewModel.requireExchangeRate(
                            from = firstCurrencyType.acronym,
                            to = firstCurrencyType.acronym
                        )
                    }
                }
            }
        }
    }

    private fun EditText.addCurrencyMask() {
        addTextChangedListener(
            object : TextWatcher {
                private var currentText = ""

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s.toString() != currentText) {
                        removeTextChangedListener(this)

                        val cleanedString = s.toString().replace("[,.]".toRegex(), "")
                        val currencyValue = cleanedString.toDoubleOrNull() ?: 0.0

                        val formattedValue = DecimalFormat(
                            "#,##0.00",
                            DecimalFormatSymbols(Locale.getDefault())
                        ).format(currencyValue / 100)
                        currentText = formattedValue
                        setText(formattedValue)
                        setSelection(formattedValue.length)

                        addTextChangedListener(this)
                    }
                }
            }
        )
    }
}