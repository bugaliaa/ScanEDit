package com.example.editeditscanner.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.example.editeditscanner.App
import com.example.editeditscanner.R
import com.example.editeditscanner.adapter.ViewFrameAdapter
import com.example.editeditscanner.data.Assets
import com.example.editeditscanner.data.Frame
import com.example.editeditscanner.databinding.ActivityViewPageBinding
import com.example.editeditscanner.viewmodels.ViewPageActivityViewModel
import com.example.editeditscanner.viewmodels.ViewPageActivityViewModelFactory
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

class ViewPageActivity : BaseActivity(), OnItemSelectedListener, ViewPager.OnPageChangeListener {

    private var docId: String? = null
    private lateinit var viewFrameAdapter: ViewFrameAdapter
    private lateinit var binding: ActivityViewPageBinding
    private lateinit var viewModel: ViewPageActivityViewModel


    private fun initialiseViewModel(docId: String) {
        (application as App).database?.let { db ->
            viewModel = ViewModelProvider(
                this,
                ViewPageActivityViewModelFactory(db.documentDao(), db.frameDao(), docId)
            ).get(ViewPageActivityViewModel::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = ""
        setSupportActionBar(findViewById(R.id.toolbar))
        if (supportActionBar != null) {
            supportActionBar?.setHomeButtonEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        docId = intent.getStringExtra("document_id")
        if (docId == null) {
            Toast.makeText(this, "Unexpected error occurred", Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        initialiseViewModel(docId!!)

        viewFrameAdapter = ViewFrameAdapter(this, ArrayList())
        viewModel.currentIndex = intent.getIntExtra(
            "frame_position",
            0
        )
        binding.let {
            it.bottomNavigationView.setOnItemSelectedListener(this)
            it.viewPager.adapter = viewFrameAdapter
            it.viewPager.addOnPageChangeListener(this)
            viewModel.frames.observe(this) { frames ->
                viewFrameAdapter.setFrames(frames)
                viewFrameAdapter.notifyDataSetChanged()
                it.viewPager.currentItem = viewModel.currentIndex
            }
        }

        Assets.extractAssets(this)

        if(!viewModel.isInitialized()) {
            val dataPath = Assets.getTessDataPath(this)
            val language = Assets.getEngLanguage()
            viewModel.initTesseract(dataPath, language, TessBaseAPI.OEM_LSTM_ONLY)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_modify -> {
                val intent = Intent(this, EditActivity::class.java)
                intent.putExtra("document_id", docId)
                intent.putExtra(
                    "frameId",
                    viewFrameAdapter.get(getCurrentIndex()).id
                )
                startActivity(intent)
            }
            R.id.menu_note -> {
                showNoteDialog(
                    "Note",
                    "Write something beautiful here! This note will be saved alongside the scanned copy",
                    viewFrameAdapter.get(getCurrentIndex()).note,
                    viewFrameAdapter.get(getCurrentIndex())
                )
            }
            R.id.menu_crop -> {
                val cropIntent = Intent(this, CropActivity::class.java)
                cropIntent.putExtra(
                    "source_path",
                    viewFrameAdapter.get(getCurrentIndex()).uri
                )
                cropIntent.putExtra(
                    "cropped_path",
                    viewFrameAdapter.get(getCurrentIndex()).croppedUri
                )
                cropIntent.putExtra(
                    "frame_position",
                    getCurrentIndex()
                )
                cropIntent.putExtra(
                    "angle",
                    viewFrameAdapter.get(getCurrentIndex()).angle
                )
                cropResultLauncher.launch(cropIntent)
            }
            R.id.menu_ocr -> {
                Toast.makeText(this, "Detecting Text. Please wait", Toast.LENGTH_SHORT).show()
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val bitmap =
                    BitmapFactory.decodeFile(viewFrameAdapter.get(getCurrentIndex()).editedUri)
                val file: File? = viewFrameAdapter.get(getCurrentIndex()).editedUri?.let {
                    File(it)
                }
                if(file != null) {
                    viewModel.recognizeImage(file)

//                    viewModel.getProcessing().observe(this) { processing ->
//
//                    }
                    viewModel.getResult().observe(this) { result ->
                        showNoteDialog("Detected text",
                        "",
                        result.toString(),
                        viewFrameAdapter.get(getCurrentIndex()))
                    }
                }
//                recognizer.process(InputImage.fromBitmap(bitmap, 0))
//                    .addOnSuccessListener { text: Text ->
//                        showNoteDialog(
//                            "Detected Text",
//                            "",
//                            text.text,
//                            viewFrameAdapter.get(getCurrentIndex())
//                        )
//                    }
//                    .addOnFailureListener {
//                        Toast.makeText(
//                            this,
//                            "ERROR: Could not detect text",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
            }
        }
        return false
    }

    var cropResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val frame = viewFrameAdapter.get(getCurrentIndex()).apply { editedUri = null }
                viewModel.updateFrame(frame)
            }
        }

    private fun getCurrentIndex(): Int {
        return binding.viewPager.currentItem
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_view_frames, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.menu_rename -> {
                showFrameRenameDialog(this, viewFrameAdapter.get(getCurrentIndex()))
            }
            R.id.menu_delete -> {
                showFrameDeleteDialog(this, viewFrameAdapter.get(getCurrentIndex()))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showFrameRenameDialog(activity: Activity, frame: Frame) {
        val frameLayout = FrameLayout(activity)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(50, 12, 50, 12)
        }
        val editText = EditText(activity).apply {
            layoutParams = params
            hint = "Frame Name"
            setText(frame.name)
        }
        frameLayout.addView(editText)
        AlertDialog.Builder(activity).apply {
            setTitle("Rename")
            setView(frameLayout)
            setNegativeButton("Cancel", null)
            setPositiveButton("Save") { _: DialogInterface?, _: Int ->
                frame.name = editText.text.toString()
                viewModel.updateFrame(frame)
            }
            create().show()
        }
    }

    private fun showFrameDeleteDialog(activity: Activity?, frame: Frame) {
        AlertDialog.Builder(activity).apply {
            setTitle("Confirm Delete")
            setMessage("Are you sure you want to delete this frame? You won't be able to recover this frame later")
            setNegativeButton("Cancel", null)
            setPositiveButton("Delete") { _, _ ->
                viewModel.deleteFrame(frame)
            }
            create().show()
        }
    }

    @SuppressLint("InflateParams")
    private fun showNoteDialog(name: String?, hint: String?, note: String?, frame: Frame) {
        layoutInflater.inflate(R.layout.dialog_note, null).apply {
            val alertDialog = AlertDialog.Builder(this@ViewPageActivity)
                .setView(this)
                .create().apply {
                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                    show()
                }
            val etNote = findViewById<EditText>(R.id.et_note).apply {
                this.hint = hint
                setText(note)
            }
            findViewById<TextView>(R.id.title).text = name
            findViewById<TextView>(R.id.tv_save).setOnClickListener {
                frame.note = etNote.text.toString()
                viewModel.updateFrame(frame)
                alertDialog.dismiss()
            }
            findViewById<TextView>(R.id.tv_cancel).setOnClickListener { alertDialog.dismiss() }
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        // required
    }

    override fun onPageSelected(position: Int) {
        viewModel.currentIndex = position
    }

    override fun onPageScrollStateChanged(state: Int) {
        // required
    }
}