package com.example.editeditscanner.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.editeditscanner.R
import com.example.editeditscanner.databinding.ActivityMainBinding
import com.example.editeditscanner.fragment.GalleryFragment
import com.example.editeditscanner.fragment.MainFragment
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

class MainActivity : BaseActivity() {
    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            if (status == SUCCESS) {
                Log.i("OpenCV", "OpenCV loaded successfully")
            } else {
                super.onManagerConnected(status)
            }
        }
    }
    private val requiredPermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private var mainFragment = MainFragment()
    private var galleryFragment = GalleryFragment()

    private lateinit var binding: ActivityMainBinding

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        title = ""
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS)
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragment_holder, mainFragment)
            .commitNow()
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val transaction = supportFragmentManager.beginTransaction()
            when (item.itemId) {
                R.id.menu_home -> {
                    transaction.replace(R.id.fragment_holder, mainFragment)
                }
                R.id.menu_gallery -> {
                    transaction.replace(R.id.fragment_holder, galleryFragment)
                }
            }
            transaction.commit()
            invalidateOptionsMenu()
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_search) {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if(mainFragment.isVisible) {
            menuInflater.inflate(R.menu.menu_fragment_main, menu)
        } else {
            menuInflater.inflate(R.menu.menu_fragment_gallery, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        val selectedItemId = binding.bottomNavigationView.selectedItemId
        if(R.id.menu_home != selectedItemId) {
            binding.bottomNavigationView.selectedItemId = R.id.menu_home
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }
}