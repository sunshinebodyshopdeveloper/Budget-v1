package com.sunshine.appsuite.budget.tools.qr

import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.cardview.widget.CardView
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.squareup.picasso.Picasso
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.orders.ServiceOrderDetailActivity
import com.sunshine.appsuite.budget.orders.data.OrderDto
import com.sunshine.appsuite.budget.orders.timeline.OtTimelineAdapter
import com.sunshine.appsuite.budget.orders.timeline.OtTimelineMapper
import com.sunshine.appsuite.budget.orders.timeline.OtTimelineRailDecoration
import com.sunshine.appsuite.budget.orders.ui.actions.OtActionCardsBinder
import com.sunshine.appsuite.budget.orders.ui.actions.QrContentToolsBinder
import java.util.Locale

class QrScanResultBottomSheet : BottomSheetDialogFragment() {

    interface Callback { fun onDismissed() }
    var callback: Callback? = null

    private var code: String = ""
    private var isOtCode: Boolean = false
    private var contactPhone: String? = null

    private var timelineAdapter: OtTimelineAdapter? = null
    private var ui: Ui? = null

    private var pendingLoadingCode: String? = null
    private var pendingOrder: OrderDto? = null

    private var currentOrder: OrderDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        code = arguments?.getString(ARG_CODE).orEmpty()
        isOtCode = arguments?.getBoolean(ARG_IS_OT) ?: false
    }

    override fun onStart() {
        super.onStart()
        configureBottomSheet()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.bottom_sheet_qr_scan_result, container, false)
        ui = Ui.bind(root)
        bindActions(root)
        setupTimeline()
        root.doOnLayout { updatePeekHeight(root) }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when {
            pendingOrder != null -> {
                val order = pendingOrder!!
                pendingOrder = null
                renderOrder(order)
            }
            pendingLoadingCode != null -> {
                val c = pendingLoadingCode!!
                pendingLoadingCode = null
                renderLoading(c)
            }
            else -> {
                if (isOtCode) renderLoading(code) else showInvalid()
            }
        }
    }

    fun showLoading(scannedCode: String) {
        code = scannedCode
        pendingOrder = null
        currentOrder = null
        ui?.let { renderLoading(scannedCode) } ?: run { pendingLoadingCode = scannedCode }
    }

    fun showOrder(order: OrderDto) {
        pendingLoadingCode = null
        currentOrder = order
        ui?.let { renderOrder(order) } ?: run { pendingOrder = order }
    }

    private fun renderLoading(scannedCode: String) {
        val u = ui ?: return
        code = scannedCode

        u.btnShare.visibility = View.GONE
        u.btnPrint.visibility = View.GONE
        u.btnCopy.visibility = View.VISIBLE
        u.chipReceivedByTow.visibility = View.GONE

        u.ivServiceOrderCover.setImageResource(R.color.google_background_settings)

        val placeholder = getString(R.string.qr_scanner_ot_sheet_ot_placeholder)
        u.tvQrRawValue.text = scannedCode
        u.tvQrNameClient.text = placeholder
        contactPhone = null
        u.tvVehicleTitle.text = placeholder

        timelineAdapter?.submitList(emptyList())
        u.rvOtTimeline.invalidateItemDecorations()
    }

    private fun renderOrder(order: OrderDto) {
        val u = ui ?: return

        u.contentGroup.visibility = View.VISIBLE
        u.btnCopy.visibility = View.VISIBLE
        u.btnShare.visibility = View.VISIBLE
        u.btnPrint.visibility = View.VISIBLE

        u.chipReceivedByTow.visibility = if (order.receivedByTow == true) View.VISIBLE else View.GONE

        val photoUrl = order.coverPhotoThumbUrl ?: order.coverPhotoUrl

        if (!photoUrl.isNullOrBlank()) {
            Picasso.get()
                .load(photoUrl)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.scrim_gradient_expressive)
                .error(R.color.google_background_settings)
                .into(u.ivServiceOrderCover)
        }

        val client = order.client?.name?.trim()?.takeIf { it.isNotBlank() }
            ?: order.clientName?.trim()?.takeIf { it.isNotBlank() }
        u.tvQrNameClient.text = client ?: "—"

        val v = order.vehicle
        u.tvVehicleTitle.text = buildVehicleTitle(v?.carBrand, v?.carName, v?.plates)

        timelineAdapter?.submitList(OtTimelineMapper.build(order))
        u.rvOtTimeline.invalidateItemDecorations()
    }

    private fun bindActions(root: View) {
        OtActionCardsBinder.bindOrderActions(
            root = root,
            getCode = { code },
            getContactPhone = { contactPhone },
            onOrderDetail = {
                val safeCode = code.trim()
                if (safeCode.isBlank()) {
                    toast(R.string.qr_scanner_invalid_qr_title)
                    return@bindOrderActions
                }
                ServiceOrderDetailActivity.Companion.start(context = requireContext(), code = safeCode)
                dismiss()
            }
        )

        QrContentToolsBinder.bindDefaults(
            root = root,
            getCode = { code },
            onClose = { dismiss() },
            getContentView = { ui?.contentGroup }
        )
    }

    private fun setupTimeline() {
        val context = requireContext()
        val u = ui ?: return
        timelineAdapter = OtTimelineAdapter(context)

        u.rvOtTimeline.apply {
            // CORRECCIÓN: Usar las propiedades de la instancia, no de la Clase
            layoutManager = LinearLayoutManager(context)
            adapter = timelineAdapter
            isNestedScrollingEnabled = false

            // CORRECCIÓN: itemDecorationCount y getItemDecorationAt son métodos de la instancia
            if ((0 until itemDecorationCount).none { getItemDecorationAt(it) is OtTimelineRailDecoration }) {
                addItemDecoration(
                    OtTimelineRailDecoration(
                        context = context,
                        getActiveIndex = { timelineAdapter?.activeIndex ?: -1 }
                    )
                )
            }
        }
    }

    fun showInvalid() {
        toast(R.string.qr_scanner_invalid_qr_title)
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback?.onDismissed()
    }

    override fun onDestroyView() {
        ui = null
        timelineAdapter = null
        super.onDestroyView()
    }

    private fun configureBottomSheet() {
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return

        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        BottomSheetBehavior.from(bottomSheet).apply {
            isDraggable = true
            skipCollapsed = false
            isFitToContents = false
            expandedOffset = 0
            peekHeight = dpToPx(260)
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun updatePeekHeight(root: View) {
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)

        val anchor = root.findViewById<View>(R.id.peekAnchor) ?: return
        val desiredPeek = anchor.bottom + dpToPx(80)

        behavior.peekHeight = desiredPeek
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun toast(@StringRes resId: Int) {
        context?.let { Toast.makeText(it, getString(resId), Toast.LENGTH_SHORT).show() }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun buildVehicleTitle(brandRaw: String?, nameRaw: String?, platesRaw: String?): CharSequence {
        val brand = brandRaw.orEmptyTrim().uppercase(Locale.getDefault())
        val name = nameRaw.orEmptyTrim().uppercase(Locale.getDefault())
        val plates = platesRaw.orEmptyTrim().uppercase(Locale.getDefault())

        val builder = SpannableStringBuilder()

        val mainInfo = when {
            brand.isNotBlank() && name.isNotBlank() -> "$brand $name"
            brand.isNotBlank() -> brand
            name.isNotBlank() -> name
            else -> ""
        }

        if (mainInfo.isNotBlank()) {
            builder.append(mainInfo)
        }

        if (plates.isNotBlank()) {
            if (builder.isNotBlank()) {
                builder.append(" | ")
            }

            val start = builder.length
            builder.append(plates)

            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return if (builder.isNotBlank()) builder else "VEHÍCULO"
    }

    private fun SpannableStringBuilder.appendBold(text: String) {
        if (text.isBlank()) return
        val start = this.length // Corrección: Referencia explícita a la longitud del builder
        append(text)
        setSpan(StyleSpan(Typeface.BOLD), start, this.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun SpannableStringBuilder.appendNormalPart(text: String) {
        if (text.isBlank()) return
        if (this.isNotEmpty()) append(" • ")
        append(text)
    }

    private fun SpannableStringBuilder.appendBoldPart(text: String) {
        if (text.isBlank()) return
        if (this.isNotEmpty()) append(" • ")
        val start = this.length
        append(text)
        setSpan(StyleSpan(Typeface.BOLD), start, this.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun String?.orEmptyTrim(): String = this?.trim() ?: ""

    private fun String.capitalizeFirst(locale: Locale): String {
        val s = trim()
        if (s.isBlank()) return ""
        return s.lowercase(locale).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
    }

    private data class Ui(
        val root: View,
        val contentGroup: View,
        val ivServiceOrderCover: ImageView,
        val tvQrRawValue: TextView,
        val tvVehicleTitle: TextView,
        val tvQrNameClient: TextView,
        val chipReceivedByTow: Chip,
        val btnCopy: CardView,
        val btnShare: CardView,
        val btnPrint: CardView,
        val rvOtTimeline: RecyclerView
    ) {
        companion object {
            fun bind(root: View): Ui = Ui(
                root = root,
                contentGroup = root.findViewById(R.id.contentGroup),
                ivServiceOrderCover = root.findViewById(R.id.ivServiceOrderCover),
                tvQrRawValue = root.findViewById(R.id.tvQrRawValue),
                tvVehicleTitle = root.findViewById(R.id.tvVehicleTitle),
                tvQrNameClient = root.findViewById(R.id.tvQrNameClient),
                chipReceivedByTow = root.findViewById(R.id.chipReceivedByTow),
                btnCopy = root.findViewById(R.id.btnCopy),
                btnShare = root.findViewById(R.id.btnShare),
                btnPrint = root.findViewById(R.id.btnPrint),
                rvOtTimeline = root.findViewById(R.id.rvOtTimeline)
            )
        }
    }

    companion object {
        private const val ARG_CODE = "arg_code"
        private const val ARG_IS_OT = "arg_is_ot"

        fun newInstance(code: String, isOt: Boolean) = QrScanResultBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_CODE, code)
                putBoolean(ARG_IS_OT, isOt)
            }
        }
    }
}