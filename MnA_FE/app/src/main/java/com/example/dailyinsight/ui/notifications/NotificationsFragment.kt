package com.example.dailyinsight.ui.notifications

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.dailyinsight.R
import com.example.dailyinsight.data.LoadResult
import com.example.dailyinsight.data.dto.IndexDto
import com.example.dailyinsight.databinding.FragmentNotificationsBinding
import com.example.dailyinsight.ui.common.setChange
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.dailyinsight.ui.common.showSnack

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationsBinding.bind(view)

        binding.swipe.setOnRefreshListener { viewModel.load(force = true) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { st ->
                when (st) {
                    is LoadResult.Loading -> binding.swipe.isRefreshing = true

                    is LoadResult.Success -> {
                        binding.swipe.isRefreshing = false
                        val list = st.data
                        val kospi  = list.find { it.name.equals("KOSPI",  true) }
                        val kosdaq = list.find { it.name.equals("KOSDAQ", true) }
                        bindIndex(binding.tvKospiName, binding.tvKospiTime, binding.tvKospiValue, binding.tvKospiChange, kospi)
                        bindIndex(binding.tvKosdaqName, binding.tvKosdaqTime, binding.tvKosdaqValue, binding.tvKosdaqChange, kosdaq)
                    }

                    is LoadResult.Empty -> {
                        binding.swipe.isRefreshing = false
                        bindIndex(binding.tvKospiName, binding.tvKospiTime, binding.tvKospiValue, binding.tvKospiChange, null)
                        bindIndex(binding.tvKosdaqName, binding.tvKosdaqTime, binding.tvKosdaqValue, binding.tvKosdaqChange, null)
                        showSnack("표시할 지수 데이터가 없습니다.")
                    }

                    is LoadResult.Error -> {
                        binding.swipe.isRefreshing = false
                        showSnack("지수 불러오기 실패: ${st.throwable.message ?: "알 수 없는 오류"}")
                    }
                }
            }
        }
    }

    private fun bindIndex(
        name: TextView,
        time: TextView,
        value: TextView,
        change: TextView,
        d: IndexDto?
    ) {
        if (d == null) {
            name.text = "--"
            value.text = "--"
            change.text = "--"
            time.text = "--:--"
            return
        }
        name.text = d.name
        value.text = String.format(Locale.KOREA, "%.2f", d.value)
        change.setChange(d.change, d.changeRate)
        time.text = d.time
    }



    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}