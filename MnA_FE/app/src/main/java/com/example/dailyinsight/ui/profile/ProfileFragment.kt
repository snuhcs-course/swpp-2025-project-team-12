package com.example.dailyinsight.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dailyinsight.databinding.FragmentProfileBinding
import com.example.dailyinsight.ui.sign.SignInActivity
import com.example.dailyinsight.ui.userinfo.ChangeNameActivity
import com.example.dailyinsight.ui.userinfo.ChangePasswordActivity
import com.example.dailyinsight.ui.userinfo.FavoriteListActivity
import com.example.dailyinsight.ui.userinfo.WithdrawActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this, ProfileViewModelFactory(requireContext()))[ProfileViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.username.observe(viewLifecycleOwner) { name ->
            binding.userName.text = name
        }

        binding.loginButton.setOnClickListener {
            val intent = Intent(requireContext(), SignInActivity::class.java)
            startActivity(intent)
        }

        binding.changeNameButton.setOnClickListener {
            val intent = Intent(requireContext(), ChangeNameActivity::class.java)
            startActivity(intent)
        }

        binding.favorite.setOnClickListener {
            val intent = Intent(requireContext(), FavoriteListActivity::class.java)
            startActivity(intent)
        }

        binding.changePasswordButton.setOnClickListener {
            val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        binding.logoutButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("네") { dialog, which ->
                    // TODO - perform logout
                    viewModel.logout()
                }
                .setNegativeButton("아니오") { dialog, which ->
                    return@setNegativeButton
                }
                .show()
        }

        binding.withdrawButton.setOnClickListener {
            val intent = Intent(requireContext(), WithdrawActivity::class.java)
            startActivity(intent)
        }

        // 로그인 상태 변화 감시
        viewModel.isLoggedIn.observe(viewLifecycleOwner) { loggedIn ->
            if (loggedIn) {
                // 로그인한 경우 → 로그인 버튼 숨기고 나머지 보이기
                binding.loginButton.visibility = View.GONE
                binding.userName.visibility = View.VISIBLE
                binding.changeNameButton.visibility = View.VISIBLE
                binding.favorite.visibility = View.VISIBLE
                binding.divider.visibility = View.VISIBLE
                binding.settings.visibility = View.VISIBLE
                binding.changePasswordButton.visibility = View.VISIBLE
                binding.logoutButton.visibility = View.VISIBLE
                binding.withdrawButton.visibility = View.VISIBLE
            } else {
                // 로그인 안 한 경우 → 로그인 버튼만 보이기
                binding.loginButton.visibility = View.VISIBLE
                binding.userName.visibility = View.GONE
                binding.changeNameButton.visibility = View.GONE
                binding.favorite.visibility = View.GONE
                binding.divider.visibility = View.GONE
                binding.settings.visibility = View.GONE
                binding.changePasswordButton.visibility = View.GONE
                binding.logoutButton.visibility = View.GONE
                binding.withdrawButton.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}