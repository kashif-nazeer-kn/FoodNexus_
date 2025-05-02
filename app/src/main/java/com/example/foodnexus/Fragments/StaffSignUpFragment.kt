package com.example.foodnexus.Fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.foodnexus.R
import com.example.foodnexus.Utils
import com.example.foodnexus.databinding.FragmentStaffSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StaffSignUpFragment : Fragment() {
    private var _binding: FragmentStaffSignUpBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        progressDialog = Dialog(requireContext()).apply {
            setContentView(R.layout.progress_bar)
            setCancelable(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigate to Login
        binding.SignupFragmentTvLoginLink.setOnClickListener {
            findNavController().navigate(R.id.action_staffSignUpFragment_to_loginFragment)
        }

        // Populate Role Spinner
        val roles = listOf("Waiter", "Chef")
        binding.SignupFragmentSpRole.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )

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
                val userId = result.user?.uid
                result.user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        saveUserToFirestore(userId!!)
                        findNavController().navigate(R.id.action_staffSignUpFragment_to_loginFragment)
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

    private fun saveUserToFirestore(uid: String) {
        val userMap = mapOf(
            "Full Name"     to binding.SignupFragmentEtStaffName.text.toString().trim(),
            "Provided ID"   to binding.SignupFragmentEtStaffId.text.toString().trim(),
            "Email"         to binding.SignupFragmentEtEmail.text.toString().trim(),
            "Phone Number"  to binding.SignupFragmentEtPhoneNumber.text.toString().trim(),
            "Role" to "Staff",
            "Category"          to binding.SignupFragmentSpRole.selectedItem.toString()
        )

        db.collection("Staff")
            .document(uid)
            .set(userMap)
            .addOnSuccessListener {
                Utils.showToast(requireContext(), "Staff account created! Please login.")
            }
            .addOnFailureListener { e ->
                Utils.showToast(requireContext(), "Failed to save staff data: ${e.message}")
            }
            .addOnCompleteListener {
                Utils.hideProgress(progressDialog)
            }
    }

    private fun validateUser(): Boolean {
        binding.apply {
            when {
                SignupFragmentEtStaffName.text.isBlank() -> {
                    SignupFragmentEtStaffName.error = "Please enter full name"
                    return false
                }
                SignupFragmentEtStaffId.text.isBlank() -> {
                    SignupFragmentEtStaffId.error = "Please enter provided ID"
                    return false
                }
                SignupFragmentEtEmail.text.isBlank() -> {
                    SignupFragmentEtEmail.error = "Please enter email"
                    return false
                }
                SignupFragmentEtPassword.text.isBlank() -> {
                    SignupFragmentEtPassword.error = "Please enter password"
                    return false
                }
                SignupFragmentEtConfirmPassword.text.isBlank() -> {
                    SignupFragmentEtConfirmPassword.error = "Please confirm password"
                    return false
                }
                SignupFragmentEtPassword.text.toString() != SignupFragmentEtConfirmPassword.text.toString() -> {
                    SignupFragmentEtConfirmPassword.error = "Passwords do not match"
                    return false
                }
                SignupFragmentEtPhoneNumber.text.isBlank() -> {
                    SignupFragmentEtPhoneNumber.error = "Please enter phone number"
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