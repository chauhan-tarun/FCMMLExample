package `in`.tarunchauhan.myaidemo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.appinvite.FirebaseAppInvite
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkInvite()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        // inflate our menu
        menuInflater.inflate(R.menu.menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        return when (item?.itemId) {

            R.id.camera -> {
                CameraHelper.dispatchTakePictureIntent(this)
                true
            }
            R.id.share -> {
                shareApp()
                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        CameraHelper.dispatchTakePictureIntent(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CameraHelper.REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            val bmp = CameraHelper.getFullSizedBitmap()

            iv_clicked_image.setImageBitmap(Bitmap.createScaledBitmap(bmp, bmp.width / 5, bmp.height / 5, true))

            val image = FirebaseVisionImage.fromBitmap(bmp)

            val detector = FirebaseVision.getInstance().visionTextDetector

            convertToText(detector, image)

        } else if (requestCode == REQUEST_INVITE && resultCode == RESULT_OK) {

            // Get the invitation IDs of all sent messages
            val ids = AppInviteInvitation.getInvitationIds(resultCode, data!!)
            for (id in ids) {
                Log.d(TAG, "onActivityResult: sent invitation $id")
            }


        }

    }

    private fun convertToText(detector: FirebaseVisionTextDetector, image: FirebaseVisionImage) {

        val result = detector.detectInImage(image)
                .addOnSuccessListener({
                    // Task completed successfully
                    extractText(it)
                })
                .addOnFailureListener(
                        {
                            // Task failed with an exception
                            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                        })
    }

    @SuppressLint("SetTextI18n")
    private fun extractText(firebaseVisionText: FirebaseVisionText) {

        tv_text.text = ""

        for (block in firebaseVisionText.blocks) {
            val boundingBox = block.boundingBox
            val cornerPoints = block.cornerPoints
            val text = block.text

            Log.d(TAG, "BLOCK : $text")

            for (line in block.lines) {

                Log.d(TAG, "Line : " + line.text)
                tv_text.text = tv_text.text.toString() + "\n" + line.text

            }

            tv_text.text = tv_text.text.toString() + "\n"
        }

    }


    private companion object {
        const val REQUEST_INVITE = 11
    }

    private fun checkInvite() {
        // Check for App Invite invitations and launch deep-link activity if possible.
        // Requires that an Activity is registered in AndroidManifest.xml to handle
        // deep-link URLs.
        FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
                .addOnSuccessListener(this, OnSuccessListener { data ->
                    if (data == null) {
                        Log.d(TAG, "getInvitation: no data")
                        return@OnSuccessListener
                    }

                    // Get the deep link
                    val deepLink = data.link

                    // Extract invite
                    val invite = FirebaseAppInvite.getInvitation(data)
                    if (invite != null) {
                        val invitationId = invite.invitationId
                        Log.d(TAG, "INVITE ID : $invitationId")
                        AlertDialog.Builder(this@MainActivity)
                                .setTitle("You have been Invited")
                                .setMessage("Congratulations as you are invited by $invitationId, we give you 500 for free :P")
                                .setCancelable(true)
                                .show()
                    }

                    // Handle the deep link
                    Log.d(TAG, "Deep Link received : $deepLink")

                })
                .addOnFailureListener(this) { e -> Log.w(TAG, "getDynamicLink:onFailure", e) }

    }

    private fun shareApp() {

        val intent = AppInviteInvitation.IntentBuilder("Invite your friends..")
                .setMessage("Hey checkout this app. It lets you convert image into text.. !!")
                .setDeepLink(Uri.parse("https://myaidemo.page.link"))
                .build()
        startActivityForResult(intent, REQUEST_INVITE)

    }
}
