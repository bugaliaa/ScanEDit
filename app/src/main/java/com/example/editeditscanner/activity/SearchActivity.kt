package com.example.editeditscanner.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.editeditscanner.App
import com.example.editeditscanner.R
import com.example.editeditscanner.adapter.DocumentsAdapter
import com.example.editeditscanner.databinding.ActivitySearchBinding
import com.example.editeditscanner.viewmodels.SearchViewModel
import com.example.editeditscanner.viewmodels.SearchViewModelFactory

class SearchActivity : BaseActivity() {
    private lateinit var documentsAdapter: DocumentsAdapter
    lateinit var viewModel: SearchViewModel

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initializeViewModel() {
        (application as App).database?.let { db ->
            viewModel = ViewModelProvider(
                this,
                SearchViewModelFactory(db.documentDao(), db.frameDao())
            ).get(SearchViewModel::class.java)
        }
    }

    private lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        if (supportActionBar != null) {
            supportActionBar?.setHomeButtonEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        initializeViewModel()
        documentsAdapter = DocumentsAdapter(this, ArrayList(), viewModel)

        binding.recyclerView.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = documentsAdapter
        }

        viewModel.documents.observe(this) { documents ->
            documentsAdapter.updateDocuments(documents)
        }

        binding.etSearch.let {
            it.requestFocus()
            it.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence?,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
                    viewModel.search(charSequence.toString())
                }

                override fun afterTextChanged(editable: Editable?) {}
            })
        }
    }
}