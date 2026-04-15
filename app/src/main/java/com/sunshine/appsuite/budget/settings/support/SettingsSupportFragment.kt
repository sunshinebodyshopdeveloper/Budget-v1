package com.sunshine.appsuite.budget.settings.support

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.databinding.FragmentSettingsSupportBinding

class SettingsSupportFragment : Fragment() {

    private var _binding: FragmentSettingsSupportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAccordions()
        setupArticleLinks()
        setupCommunityCard()
        setupSearch()
    }

    private fun setupSearch() {
        binding.etSupportSearch.setOnEditorActionListener { v, _, _ ->
            val q = v.text?.toString()?.trim().orEmpty()
            if (q.isNotEmpty()) {
                openUrl("https://support.sunshineappsuite.com/search?q=" + Uri.encode(q))
            }
            true
        }
    }

    private fun setupAccordions() {
        toggleable(
            header = binding.rowPopularHeader,
            body = binding.rowPopularBody,
            chevron = binding.ivPopularChevron,
            expandedByDefault = true
        )

        toggleable(
            header = binding.rowTroubleshootHeader,
            body = binding.rowTroubleshootBody,
            chevron = binding.ivTroubleshootChevron
        )

        toggleable(
            header = binding.rowConfigHeader,
            body = binding.rowConfigBody,
            chevron = binding.ivConfigChevron
        )
    }

    private fun toggleable(
        header: View,
        body: View,
        chevron: ImageView,
        expandedByDefault: Boolean = false
    ) {
        fun setExpanded(expanded: Boolean) {
            body.visibility = if (expanded) View.VISIBLE else View.GONE
            chevron.animate().rotation(if (expanded) 180f else 0f).setDuration(150).start()
            header.isSelected = expanded
        }

        setExpanded(expandedByDefault)

        header.setOnClickListener {
            val expanded = body.visibility == View.VISIBLE
            setExpanded(!expanded)
        }
    }

    private fun setupArticleLinks() {
        val items = listOf(
            binding.tvPopular1,
            binding.tvPopular2,
            binding.tvPopular3,
            binding.tvTrouble1,
            binding.tvTrouble2,
            binding.tvConfig1
        )

        items.forEach { tv ->
            tv.setOnClickListener {
                val url = (tv.tag as? String).orEmpty()
                if (url.isNotBlank()) openUrl(url)
            }
        }
    }

    private fun setupCommunityCard() {
        binding.cardCommunity.setOnClickListener {
            openUrl(getString(R.string.support_url_community))
        }
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
