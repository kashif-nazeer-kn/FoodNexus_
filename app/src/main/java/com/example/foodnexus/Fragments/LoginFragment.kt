package com.example.foodnexus.Fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.foodnexus.R
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var preferences: SharedPreferences
    private lateinit var progressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        preferences = requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)

        progressDialog = Dialog(requireContext()).apply {
            setContentView(R.layout.progress_bar)
            setCancelable(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.LoginFragmentTvSignUpLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_roleAssignFragment)
        }

        binding.LoginFragmentTvForgotPassword.setOnClickListener {
            val email = binding.LoginFragmentEtEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.LoginFragmentEtEmail.error = "Enter your email first"
                return@setOnClickListener
            }

            Utils.showProgress(progressDialog)
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Utils.showToast(requireContext(), "Password reset email sent.")
                }
                .addOnFailureListener { e ->
                    Utils.showToast(requireContext(), "Failed: ${e.message}")
                }
                .addOnCompleteListener {
                    Utils.hideProgress(progressDialog)
                }
        }

        binding.LoginFragmentBtnLogin.setOnClickListener {
            val email = binding.LoginFragmentEtEmail.text.toString().trim()
            val password = binding.LoginFragmentEtPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.LoginFragmentEtEmail.error = "Enter email"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.LoginFragmentEtPassword.error = "Enter password"
                return@setOnClickListener
            }

            Utils.showProgress(progressDialog)

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val user = auth.currentUser
                    val userId = user?.uid

                    if (user != null && userId != null) {
                        if (!user.isEmailVerified) {
                            Utils.showToast(requireContext(), "Please verify your email before login.")
                            auth.signOut()
                            Utils.hideProgress(progressDialog)
                            return@addOnSuccessListener
                        }

                        firestore.collection("Restaurants").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val role = document.getString("Role")
                                    val editor = preferences.edit()
                                    editor.putBoolean("isLogin", true)
                                    editor.putString("userId", userId)

                                    when (role) {
                                        "Owner" -> {
                                            editor.putString("role",role)
                                            editor.apply()
                                            findNavController().navigate(R.id.action_loginFragment_to_restaurantMenuFragment)
                                        }

                                        "Staff" -> {
                                            val category = document.getString("Category")
                                            val ownerId = document.getString("Provided ID")
                                            editor.putString("ownerId", ownerId)

                                            if(category=="Waiter")
                                            {
                                                editor.putString("role",category)
                                                editor.apply()
                                                findNavController().navigate(R.id.action_loginFragment_to_waiterTableManageFragment)

                                            }
                                            else if (category=="Chef")
                                            {
                                                editor.putString("role",category)
                                                editor.apply()
                                                findNavController().navigate(R.id.action_loginFragment_to_chefOrderReceivingFragment)

                                            }


                                        }

                                        else -> {
                                            Utils.showToast(requireContext(), "Unknown role.")
                                        }
                                    }
                                } else {
                                    Utils.showToast(requireContext(), "User data not found in Firestore.")
                                }
                            }
                            .addOnFailureListener { e ->
                                Utils.showToast(requireContext(), "Firestore error: ${e.message}")
                            }
                            .addOnCompleteListener {
                                Utils.hideProgress(progressDialog)
                            }

                    } else {
                        Utils.showToast(requireContext(), "Login failed. User not found.")
                        Utils.hideProgress(progressDialog)
                    }
                }
                .addOnFailureListener { e ->
                    Utils.showToast(requireContext(), "Login failed: ${e.message}")
                    Utils.hideProgress(progressDialog)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
