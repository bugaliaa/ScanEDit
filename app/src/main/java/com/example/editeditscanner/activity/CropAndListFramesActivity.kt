package com.example.editeditscanner.activity

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.editeditscanner.App
import com.example.editeditscanner.R
import com.example.editeditscanner.adapter.ProgressFramesAdapter
import com.example.editeditscanner.databinding.ActivityCropAndListFramesBinding
import com.example.editeditscanner.utils.Utils
import com.example.editeditscanner.viewmodels.CropAndListFramesViewModel
import com.example.editeditscanner.viewmodels.CropAndListFramesViewModelFactory

class CropAndListFramesActivity : BaseActivity() {
    private lateinit var viewModel: CropAndListFramesViewModel

    private var resulteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.exportPdf(uri)
                }
            }
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list_frames, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export_pdf -> {
                Utils.sendCreateFileIntent(
                    viewModel.document.name!!,
                    "application/pdf",
                    resulteLauncher
                )
            }
            R.id.menu_delete -> {
                viewModel.showConfirmDeleteDialog(this)
            }
            R.id.menu_rename -> {
                viewModel.showRenameDialog()
            }
            androidx.appcompat.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val binding = ActivityCropAndListFramesBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        title = ""
        setSupportActionBar(findViewById(R.id.toolbar))
        if (supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeButtonEnabled(true)
        }
        val sourcePaths = intent.getStringArrayListExtra("uris") ?: ArrayList()
        binding.fab.visibility = View.GONE
        initializeViewModel()

        val framesAdapter = ProgressFramesAdapter(this, viewModel.document.id, ArrayList())
        binding.rvFrames.let {
            it.layoutManager = GridLayoutManager(this, 2)
            it.setHasFixedSize(true)
            it.adapter = framesAdapter
        }

        viewModel.let {
            it.setup(sourcePaths)
            it.frames.observe(this) { frames ->
                framesAdapter.frames = frames
                framesAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun initializeViewModel() {
        (application as App).database?.let { db ->
            viewModel = ViewModelProvider(
                this,
                CropAndListFramesViewModelFactory(
                    application as App,
                    db.documentDao(),
                    db.frameDao()
                )
            ).get(CropAndListFramesViewModel::class.java)
        }
    }
}