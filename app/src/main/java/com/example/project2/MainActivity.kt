
package com.example.project2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var companyNameTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        companyNameTextView = findViewById(R.id.companyNameTextView)
            //result launch, get the name of company
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null) {
                    val contactUri: Uri? = data.data
                    if (contactUri != null) {
                        val company = extractCompanyName(contactUri)
                        companyNameTextView.text = "Company: $company"
                    }
                }
            }
        }
// request permission on click
        val pickContactButton: Button = findViewById(R.id.pickContactButton)
        pickContactButton.setOnClickListener {
            if (hasContactsPermission()) {
                val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                resultLauncher.launch(pickContactIntent)
            } else {
                requestContactsPermission()
            }
        }
    }
//has permission to read contact or not from "Manifest.permission.READ_CONTACTS"
    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }
//requesting for permission, triggering dialogue box
    private fun requestContactsPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), CONTACTS_PERMISSION_REQUEST)
    }

    //this func called automatically after user respond to dialogue box
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { //which permission's dialogue box will pop up
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_PERMISSION_REQUEST) {
            //launch the contact picker to pick any contact if it's not empty
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                resultLauncher.launch(pickContactIntent)//launch
            }
        }
    }

    @SuppressLint("Range")

    private fun extractCompanyName(contactUri: Uri): String? { //uri of the selected contact
        val contentResolver: ContentResolver = applicationContext.contentResolver //query content resolver for the data
        val cursor: Cursor? = contentResolver.query(contactUri, null, null, null, null)  //query contact resolver for the data
        //function continues to find the company name and print null if cant find it
        cursor?.use {
            if (it.moveToFirst()) { //check for rows
                val id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID)) //get contact id
                val companyNameCursor: Cursor? = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?", //filters the data rows to those that match the contact's ID and have the MIME type of organization data
                    arrayOf(id, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                    null
                )

                companyNameCursor?.use { companyCursor ->
                    if (companyCursor.moveToFirst()) { //check for sub rows if presents, if it does move the cursor there
                        return companyCursor.getString(companyCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY))
                    }
                }
            }
        }

        return null
    }
}

const val CONTACTS_PERMISSION_REQUEST = 1


