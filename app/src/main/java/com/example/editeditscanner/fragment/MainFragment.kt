package com.example.editeditscanner.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.editeditscanner.App
import com.example.editeditscanner.R
import com.example.editeditscanner.activity.ScanActivity
import com.example.editeditscanner.adapter.DocumentsAdapter
import com.example.editeditscanner.viewmodels.MainActivityViewModel
import com.example.editeditscanner.viewmodels.MainActivityViewModelFactory

class MainFragment : Fragment() {

    private lateinit var documentsAdapter: DocumentsAdapter
    private lateinit var viewModel: MainActivityViewModel

    private fun initialiseViewModel() {
        activity?.let { activity ->
            (activity.application as App).database?.let { db ->
                viewModel = ViewModelProvider(
                    activity,
                    MainActivityViewModelFactory(db.documentDao(), db.frameDao())
                ).get(MainActivityViewModel::class.java)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_main, container, false)
        val fab = v.findViewById<View?>(R.id.fab)
        fab.setOnClickListener {
            startActivity(Intent(requireActivity(), ScanActivity::class.java))
        }

        initialiseViewModel()

        documentsAdapter = DocumentsAdapter(requireActivity(), emptyList(), viewModel)

        v.findViewById<RecyclerView>(R.id.recycler_view).let {
            it.setHasFixedSize(true)
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = documentsAdapter
            viewModel.getAllDocuments()?.observe(viewLifecycleOwner) { documents ->
                documentsAdapter.updateDocuments(documents)
            }
        }
        return v
    }
}