package android.example.konnectme

import android.content.Intent
import android.example.konnectme.daos.UserDao
import android.example.konnectme.models.User
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_sign_in.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.item_post.*
import kotlinx.android.synthetic.main.signup_tab_fragment.*

class SignInActivity : AppCompatActivity() {

    private val RC_SIGN_IN: Int = 1234
    private val TAG = "SignInActivity Tag"
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter:FragmentAdapter
    private lateinit var viewPager:ViewPager2
    private val types="Google"
    private var boolname=false
    private var boolimg=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = Firebase.auth

        fab_google.setOnClickListener {

            signIn()
        }
        adapter = FragmentAdapter(this)
        viewPager=findViewById(R.id.view_pager)
        viewPager.adapter=adapter

        val tabArray = arrayOf("Login","Signup")
        val tabLayout:TabLayout
        tabLayout=findViewById(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager){
            tab,position -> tab.text=tabArray[position]
        }.attach()
    }

    fun signUpMethod(view:View){
        boolname=true
        boolimg=true
        val name=username.text.toString()
        val email=email.text.toString()
        val password=password.text.toString()
        val conPassword = confirm_password.text.toString();
        if(name.isEmpty()){
            Toast.makeText(this,"please enter Username ",Toast.LENGTH_SHORT).show()
        }
        else if(email.isEmpty()){
            Toast.makeText(this,"please enter Email ",Toast.LENGTH_SHORT).show()
        }
        else if(password.isEmpty()){
            Toast.makeText(this,"please enter Password ",Toast.LENGTH_SHORT).show()
        }
        else if(password.length<=6){
            Toast.makeText(this,"password length must be greater than 6",Toast.LENGTH_SHORT).show()
        }
        else if(conPassword.isEmpty()){
            Toast.makeText(this,"please enter Password ",Toast.LENGTH_SHORT).show()
        }
        else if(password!=conPassword){
            Toast.makeText(this,"passwords don't match",Toast.LENGTH_SHORT).show()
        }
        else{
            auth.createUserWithEmailAndPassword(email.trim(),password).addOnCompleteListener(this){
                task ->
                if(task.isSuccessful){
                    val user=auth.currentUser
                    updateUI(user)
                }
                else{
                    if(task.exception.toString()=="com.google.firebase.auth.FirebaseAuthUserCollisionException: The email address is already in use by another account."){
                        Toast.makeText(this,"Email address already in use",Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(this, "invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun logInMethod(view:View){
        val email=email.text.toString()
        val password=password.text.toString()
        if(email.isEmpty()){
            Toast.makeText(this,"please enter Email ",Toast.LENGTH_SHORT).show()
        }
        else if(password.isEmpty()){
            Toast.makeText(this,"please enter Password ",Toast.LENGTH_SHORT).show()
        }
        else{
            auth.signInWithEmailAndPassword(email.trim(),password).addOnCompleteListener(this){
                task ->
                if(task.isSuccessful){
                    val user=auth.currentUser
                    updateUI(user)
                }
                else{
                    Toast.makeText(this,"invalid email or password",Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account =
                completedTask.getResult(ApiException::class.java)!!
            Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)

        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        fab_google.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.IO) {
            val auth = auth.signInWithCredential(credential).await()
            val firebaseUser = auth.user
            withContext(Dispatchers.Main) {
                updateUI(firebaseUser)
            }
        }

    }

    private fun updateUI(firebaseUser: FirebaseUser?) {
        if(firebaseUser != null) {
            var name=firebaseUser.displayName
            var imgUrl= firebaseUser.photoUrl.toString()
            if(boolname){
                name=username.text.toString()
            }
            if(boolimg){
                imgUrl="https://firebasestorage.googleapis.com/v0/b/konnectme-14bb9.appspot.com/o/acc.png?alt=media&token=76ed907c-c4fc-4416-987f-a165e842de82"
            }

            val user = User(firebaseUser.uid,name,imgUrl)
            val usersDao = UserDao()
            usersDao.addUser(user)

            val mainActivityIntent = Intent(this, MainActivity::class.java)
            startActivity(mainActivityIntent)
            finish()
        } else {
            fab_google.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }
}