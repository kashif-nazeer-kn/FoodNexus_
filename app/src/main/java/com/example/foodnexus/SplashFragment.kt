package com.example.foodnexus

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.foodnexus.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {
    private lateinit var binding: FragmentSplashBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentSplashBinding.inflate(layoutInflater)
        preferences=requireContext().getSharedPreferences("Login Check",Context.MODE_PRIVATE)
        binding.SplashFragmentBtnLogin.visibility=View.GONE
        binding.SplashFragmentBtnSignUp.visibility=View.GONE
        binding.SplashFragmentTvOr.visibility=View.GONE
        val isLogin=preferences.getBoolean("isLogin",false)

        if(isLogin)
        {
            Handler(Looper.getMainLooper()).postDelayed({

                val navHostFragment = parentFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
                val navController = navHostFragment.navController

                // Set new nav graph (home)
                navController.setGraph(R.navigation.main_nav_graph)

            },500)
        }
        else {
            binding.SplashFragmentBtnLogin.visibility=View.VISIBLE
            binding.SplashFragmentBtnSignUp.visibility=View.VISIBLE
            binding.SplashFragmentTvOr.visibility=View.VISIBLE

            binding.SplashFragmentBtnLogin.setOnClickListener {
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            }
            binding.SplashFragmentBtnSignUp.setOnClickListener {
                findNavController().navigate(R.id.action_roleAssignFragment_to_signUpFragment)
            }

        }

        return binding.root
    }

}