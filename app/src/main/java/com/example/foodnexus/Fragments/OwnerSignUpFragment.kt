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
import com.example.foodnexus.databinding.FragmentOwnerSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OwnerSignUpFragment : Fragment() {
    private var _binding: FragmentOwnerSignUpBinding? = null
    private lateinit var progressDialog: Dialog
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        preferences=requireContext().getSharedPreferences("Details", Context.MODE_PRIVATE)

        progressDialog = Dialog(requireContext())
        progressDialog.setContentView(R.layout.progress_bar)
        progressDialog.setCancelable(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.SignupFragmentTvLoginLink.setOnClickListener {
            findNavController().navigate(R.id.action_ownerSignUpFragment_to_loginFragment)
        }
        binding.SignupFragmentBtnSignUp.setOnLongClickListener {
            findNavController().navigate(R.id.action_ownerSignUpFragment_to_restaurantMenuFragment)
            true
        }

        binding.SignupFragmentBtnSignUp.setOnClickListener {
            if (validateUser()) {
                performSignUp()
            }
        }
    }

    private fun performSignUp() {
        val email = binding.SignupFragmentEtEmail.text.toString().trim()
        val password = binding.SignupFragmentEtPassword.text.toString().trim()

        Utils.showProgress(progressDialog)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId= result.user?.uid
                result.user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        saveUserToFirestore()
                        findNavController().navigate(R.id.action_ownerSignUpFragment_to_loginFragment)
                        preferences.edit().apply(){
                            putString("userId",userId)
                            apply()
                        }
                    }
                    ?.addOnFailureListener { e ->
                        Utils.showToast(
                            requireContext(),
                            "Failed to send verification email: ${e.message}"
                        )
                        Utils.hideProgress(progressDialog)
                    }
                Utils.showToast(requireContext(), "Account created! Verification email sent.")
            }
            .addOnFailureListener { e ->
                Utils.hideProgress(progressDialog)
                Utils.showToast(requireContext(), "Sign up failed: ${e.message}")
            }
    }

    private fun saveUserToFirestore() {
        val uid = auth.currentUser?.uid ?: return
        val userMap = mapOf(
            "Owner Name" to binding.SignupFragmentEtOwnerName.text.toString().trim(),
            "Email" to binding.SignupFragmentEtEmail.text.toString().trim(),
            "Phone Number" to binding.SignupFragmentEtPhoneNumber.text.toString().trim(),
            "Restaurant Name" to binding.SignupFragmentEtRestaurantName.text.toString().trim(),
            "Address" to binding.SignupFragmentEtAddress.text.toString().trim(),
            "Provided ID" to binding.SignupFragmentEtId.text.toString().trim()
        )

        db.collection("Restaurants")
            .document(uid)
            .set(userMap)
            .addOnSuccessListener {
                Utils.showToast(requireContext(), "Account successfully created Please Login")
            }
            .addOnFailureListener { e ->
                Utils.showToast(requireContext(), "Failed to save user data: ${e.message}")
            }
            .addOnCompleteListener {
                Utils.hideProgress(progressDialog)
            }
    }

    private fun validateUser(): Boolean {
        binding.apply {
            when {
                SignupFragmentEtOwnerName.text.toString().trim().isEmpty() -> {
                    SignupFragmentEtOwnerName.error = "Please enter your name"
                    return false
                }
                SignupFragmentEtEmail.text.toString().trim().isEmpty() -> {
                    SignupFragmentEtEmail.error = "Please enter your email"
                    return false
                }
                SignupFragmentEtPassword.text.toString().trim().isEmpty() -> {
                    SignupFragmentEtPassword.error = "Please enter your password"
                    return false
                }
                SignupFragmentEtConfirmPassword.text.toString().trim().isEmpty() -> {
                    SignupFragmentEtConfirmPassword.error = "Please confirm your password"
                    return false
                }
                SignupFragmentEtPassword.text.toString() != SignupFragmentEtConfirmPassword.text.toString() -> {
                    SignupFragmentEtConfirmPassword.error = "Passwords do not match"
                    return false
                }
                SignupFragmentEtPhoneNumber.text.toString().trim().isEmpty() -> {
                    SignupFragmentEtPhoneNumber.error = "Please enter your phone number"
                    return false
                }
                SignupFragmentEtRestaurantName.text.toString().trim().isEmpty() -> {
                    SignupFragmentEtRestaurantName.error = "Please enter your restaurant name"
                    return false
                }
                SignupFragmentEtAddress.text.toString().trim().isEmpty() -> {
                    SignupFragmentEtAddress.error = "Please enter your address"
                    return false
                }
                SignupFragmentEtId.text.toString().trim().isEmpty() -> {
                    SignupFragmentEtId.error = "Please enter your ID"
                    return false
                }
                else -> return true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}