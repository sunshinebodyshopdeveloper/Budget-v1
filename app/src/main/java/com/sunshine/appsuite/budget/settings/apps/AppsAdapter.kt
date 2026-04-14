package com.sunshine.appsuite.budget.settings.apps

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.sunshine.appsuite.R

class AppsAdapter(
    private val items: MutableList<AppModule>,
    private val listener: ModuleActionListener
) : RecyclerView.Adapter<AppsAdapter.AppModuleViewHolder>() {

    interface ModuleActionListener {
        fun onPrimaryActionClick(module: AppModule)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppModuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_module, parent, false)
        return AppModuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppModuleViewHolder, position: Int) {
        val module = items[position]
        holder.bind(module)
    }

    override fun getItemCount(): Int = items.size

    inner class AppModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardApp: MaterialCardView = itemView.findViewById(R.id.cardApp)
        private val layoutAppHeader: View = itemView.findViewById(R.id.layoutAppHeader)
        private val layoutAppExtra: View = itemView.findViewById(R.id.layoutAppExtra)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val ivAppExpandArrow: ImageView = itemView.findViewById(R.id.ivAppExpandArrow)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvAppDescription: TextView = itemView.findViewById(R.id.tvAppDescription)
        private val chipAppStatus: Chip = itemView.findViewById(R.id.chipAppStatus)
        private val ivAppIconStatus: ImageView = itemView.findViewById(R.id.ivAppIconStatus)
        private val tvInstallApp: TextView = itemView.findViewById(R.id.tvInstallApp)
        private val tvSizeApp: TextView = itemView.findViewById(R.id.tvSizeApp)
        private val tvStatusAvailable: TextView = itemView.findViewById(R.id.tvStatusAvailable)
        private val btnAppPrimary: MaterialButton = itemView.findViewById(R.id.btnAppPrimary)

        fun bind(module: AppModule) {
            val context = itemView.context

            ivAppIcon.setImageResource(module.iconRes)
            tvAppName.setText(module.nameRes)
            tvAppDescription.setText(module.descriptionRes)
            tvSizeApp.text = module.sizeText

            // Expand / collapse
            layoutAppExtra.visibility = if (module.isExpanded) View.VISIBLE else View.GONE
            ivAppExpandArrow.rotation = if (module.isExpanded) 180f else 0f

            // Estado instalada / no instalada
            if (module.isInstalled) {
                chipAppStatus.text = context.getString(R.string.apps_status_installed)
                chipAppStatus.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.apps_chip_installed_bg)
                )
                chipAppStatus.setTextColor(
                    ContextCompat.getColor(context, R.color.apps_chip_installed_text)
                )

                ivAppIconStatus.setImageResource(R.drawable.ic_check_circle)
                ivAppIconStatus.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.google_green)
                )
                tvInstallApp.text = context.getString(R.string.apps_install_state_ready)
                tvStatusAvailable.text = context.getString(R.string.apps_status_available)
            } else {
                chipAppStatus.text = context.getString(R.string.apps_status_not_installed)
                chipAppStatus.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.apps_chip_not_installed_bg)
                )
                chipAppStatus.setTextColor(
                    ContextCompat.getColor(context, R.color.apps_chip_not_installed_text)
                )

                ivAppIconStatus.setImageResource(R.drawable.ic_check_circle)
                ivAppIconStatus.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.google_text_secondary)
                )
                tvInstallApp.text = context.getString(R.string.apps_install_state_pending)
                tvStatusAvailable.text = context.getString(R.string.apps_status_not_available)
            }

            // Botón principal
            btnAppPrimary.text = if (module.isInstalled) {
                context.getString(R.string.apps_action_open)
            } else {
                context.getString(R.string.apps_action_get)
            }

            btnAppPrimary.setOnClickListener {
                listener.onPrimaryActionClick(module)
            }

            // Toggle expand
            val toggleListener = View.OnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position]
                    item.isExpanded = !item.isExpanded
                    notifyItemChanged(position)
                }
            }

            cardApp.setOnClickListener(toggleListener)
            layoutAppHeader.setOnClickListener(toggleListener)
            ivAppExpandArrow.setOnClickListener(toggleListener)
        }
    }
}