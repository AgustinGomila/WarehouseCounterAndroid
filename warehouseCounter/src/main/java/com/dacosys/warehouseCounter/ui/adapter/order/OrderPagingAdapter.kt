package com.dacosys.warehouseCounter.ui.adapter.order

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.GONE
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.VISIBLE
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderStatus
import com.dacosys.warehouseCounter.databinding.OrderRowBinding
import com.dacosys.warehouseCounter.databinding.OrderRowExpandedBinding
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor

class OrderPagingAdapter private constructor(builder: Builder) :
    PagingDataAdapter<OrderResponse, ViewHolder>(ItemDiffUtilCallback) {

    private var currentIndex = NO_POSITION

    fun currentItem(): OrderResponse? {
        return getItem(currentIndex)
    }

    private var selectedItemChangedListener: SelectedItemChangedListener? = null

    interface SelectedItemChangedListener {
        fun onSelectedItemChanged(item: OrderResponse?)
    }

    fun destroyListeners() {
        this.selectedItemChangedListener = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            SELECTED_VIEW_TYPE -> {
                SelectedViewHolder(
                    OrderRowExpandedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                UnselectedViewHolder(
                    OrderRowBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (currentIndex == holder.bindingAdapterPosition) {
                currentIndex = NO_POSITION
                notifyItemSelectedChanged(holder.bindingAdapterPosition)
            } else {
                val previousSelectedItemPosition = currentIndex
                currentIndex = holder.bindingAdapterPosition
                notifyItemSelectedChanged(currentIndex)

                if (previousSelectedItemPosition != NO_POSITION) {
                    notifyItemSelectedChanged(previousSelectedItemPosition)
                }
            }

            holder.itemView.isSelected = currentIndex == position
        }

        if (currentIndex == position) {
            setSelectedHolder(holder as SelectedViewHolder, position)
        } else {
            setUnselectedHolder(holder as UnselectedViewHolder, position)
        }
    }

    private fun setSelectedHolder(holder: SelectedViewHolder, position: Int) {
        val item = getItem(position) ?: return

        holder.bind(item = item)

        holder.itemView.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            getColorWithAlpha(colorId = R.color.lightslategray, alpha = 220), BlendModeCompat.MODULATE
        )
    }

    private fun setUnselectedHolder(holder: UnselectedViewHolder, position: Int) {
        val item = getItem(position) ?: return

        holder.bind(item = item)

        holder.itemView.background.colorFilter = null
    }

    // El método getItemViewType devuelve el tipo de vista que se usará para el elemento en la posición dada
    override fun getItemViewType(position: Int): Int {
        return if (currentIndex == position) SELECTED_VIEW_TYPE
        else UNSELECTED_VIEW_TYPE
    }

    private fun notifyItemSelectedChanged(pos: Int) {
        notifyItemChanged(currentIndex)
        var item: OrderResponse? = null
        if (pos != NO_POSITION && pos < itemCount) item = getItem(pos)
        selectedItemChangedListener?.onSelectedItemChanged(item)
    }

    internal class SelectedViewHolder(val binding: OrderRowExpandedBinding) :
        ViewHolder(binding.root) {

        fun bind(item: OrderResponse) {
            binding.checkBoxConstraintLayout.visibility = GONE

            binding.descriptionTextView.text =
                item.description.ifEmpty { context.getString(R.string.without_description) }

            val extId = item.externalId
            if (extId.isNotEmpty()) {
                binding.extIdTextView.text = extId
                binding.extIdPanel.visibility = VISIBLE
            } else {
                binding.extIdTextView.text = ""
                binding.extIdPanel.visibility = GONE
            }

            val status = item.status.description
            if (status.isNotEmpty()) {
                binding.statusTextView.text = status
                binding.statusPanel.visibility = VISIBLE
            } else {
                binding.statusTextView.text = ""
                binding.statusPanel.visibility = GONE
            }

            val type = item.orderType.description
            if (type.isNotEmpty()) {
                binding.orderTypeTextView.text = type
                binding.typePanel.visibility = VISIBLE
            } else {
                binding.orderTypeTextView.text = ""
                binding.typePanel.visibility = GONE
            }

            if (type.isEmpty() && status.isEmpty()) binding.orderTypeStatusPanel.visibility = GONE
            else binding.orderTypeStatusPanel.visibility = VISIBLE

            val zone = item.zone
            if (extId.isNotEmpty()) {
                binding.zoneTextView.text = zone
                binding.zonePanel.visibility = VISIBLE
            } else {
                binding.zoneTextView.text = ""
                binding.zonePanel.visibility = GONE
            }

            val startDate = item.startDate
            val finishDate = item.finishDate ?: ""

            if (!startDate.isNullOrEmpty() || finishDate.isNotEmpty()) binding.datesPanel.visibility = VISIBLE
            else binding.datesPanel.visibility = GONE

            binding.creationDateTextView.text = startDate
            binding.finishDateTextView.text = finishDate.ifEmpty { context.getString(R.string.uncompleted) }

            setStyle(item)
        }

        private fun setStyle(item: OrderResponse) {
            val v = itemView

            // Background layouts
            // Resalta por estado del ítem
            val layoutApproved = getDrawable(context.resources, R.drawable.layout_thin_border_status_approved, null)
            val layoutDraft = getDrawable(context.resources, R.drawable.layout_thin_border_status_draft, null)
            val layoutInProcess = getDrawable(context.resources, R.drawable.layout_thin_border_status_in_process, null)
            val layoutInTransit = getDrawable(context.resources, R.drawable.layout_thin_border_status_in_transit, null)
            val layoutDelivered = getDrawable(context.resources, R.drawable.layout_thin_border_status_delivered, null)
            val layoutFinished = getDrawable(context.resources, R.drawable.layout_thin_border_status_finished, null)
            val layoutPending = getDrawable(context.resources, R.drawable.layout_thin_border_status_pending, null)
            val layoutPendingDistribution =
                getDrawable(context.resources, R.drawable.layout_thin_border_status_pending_distribution, null)
            val layoutProcessed = getDrawable(context.resources, R.drawable.layout_thin_border_status_processed, null)
            val layoutNoStatus = getDrawable(context.resources, R.drawable.layout_thin_border_status_no_status, null)
            val layoutActive = getDrawable(context.resources, R.drawable.layout_thin_border_status_active, null)
            val layoutDeactivated =
                getDrawable(context.resources, R.drawable.layout_thin_border_status_deactivated, null)
            val layoutFlashing = getDrawable(context.resources, R.drawable.layout_thin_border_status_flashing, null)

            val backColor: Drawable
            val foreColor: Int

            when (item.status) {
                OrderStatus.approved -> {
                    backColor = layoutApproved!!
                    foreColor = approvedSelectedForeColor
                }

                OrderStatus.active -> {
                    backColor = layoutActive!!
                    foreColor = activeSelectedForeColor
                }

                OrderStatus.draft -> {
                    backColor = layoutDraft!!
                    foreColor = draftSelectedForeColor
                }

                OrderStatus.deactivated -> {
                    backColor = layoutDeactivated!!
                    foreColor = deactivatedSelectedForeColor
                }

                OrderStatus.inProcess -> {
                    backColor = layoutInProcess!!
                    foreColor = inProcessSelectedForeColor
                }

                OrderStatus.inTransit -> {
                    backColor = layoutInTransit!!
                    foreColor = inTransitSelectedForeColor
                }

                OrderStatus.delivered -> {
                    backColor = layoutDelivered!!
                    foreColor = deliveredSelectedForeColor
                }

                OrderStatus.finished -> {
                    backColor = layoutFinished!!
                    foreColor = finishedSelectedForeColor
                }

                OrderStatus.pending -> {
                    backColor = layoutPending!!
                    foreColor = pendingSelectedForeColor
                }

                OrderStatus.pendingDistribution -> {
                    backColor = layoutPendingDistribution!!
                    foreColor = pendingDistributionSelectedForeColor
                }

                OrderStatus.processed -> {
                    backColor = layoutProcessed!!
                    foreColor = processedSelectedForeColor
                }

                OrderStatus.flashing -> {
                    backColor = layoutFlashing!!
                    foreColor = flashingSelectedForeColor
                }

                else -> {
                    backColor = layoutNoStatus!!
                    foreColor = noStatusSelectedForeColor
                }
            }

            val titleForeColor: Int = manipulateColor(foreColor, 0.8f)

            v.background = backColor

            // Values
            binding.descriptionTextView.setTextColor(foreColor)

            binding.statusTextView.setTextColor(foreColor)
            binding.orderTypeTextView.setTextColor(foreColor)
            binding.extIdTextView.setTextColor(foreColor)
            binding.zoneTextView.setTextColor(foreColor)
            binding.creationDateTextView.setTextColor(foreColor)
            binding.finishDateTextView.setTextColor(foreColor)

            // Labels
            binding.statusLabelTextView.setTextColor(titleForeColor)
            binding.orderTypeLabelTextView.setTextColor(titleForeColor)
            binding.extIdLabelTextView.setTextColor(titleForeColor)
            binding.zoneLabelTextView.setTextColor(titleForeColor)
            binding.creationDateLabelTextView.setTextColor(titleForeColor)
            binding.finishDateLabelTextView.setTextColor(titleForeColor)

            // CheckBox
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)
        }
    }

    internal class UnselectedViewHolder(val binding: OrderRowBinding) :
        ViewHolder(binding.root) {
        fun bind(item: OrderResponse) {
            binding.checkBoxConstraintLayout.visibility = GONE

            binding.descriptionTextView.text =
                item.description.ifEmpty { context.getString(R.string.without_description) }
            binding.extIdTextView.text = item.externalId
            binding.creationDateTextView.text = item.startDate

            setStyle(item)
        }

        private fun setStyle(item: OrderResponse) {
            val v = itemView

            val titleForeColor: Int = darkslategray

            // Background layouts
            // Resalta por estado del ítem
            val layoutApproved = getDrawable(context.resources, R.drawable.layout_thin_border_status_approved, null)
            val layoutDraft = getDrawable(context.resources, R.drawable.layout_thin_border_status_draft, null)
            val layoutInProcess = getDrawable(context.resources, R.drawable.layout_thin_border_status_in_process, null)
            val layoutInTransit = getDrawable(context.resources, R.drawable.layout_thin_border_status_in_transit, null)
            val layoutDelivered = getDrawable(context.resources, R.drawable.layout_thin_border_status_delivered, null)
            val layoutFinished = getDrawable(context.resources, R.drawable.layout_thin_border_status_finished, null)
            val layoutPending = getDrawable(context.resources, R.drawable.layout_thin_border_status_pending, null)
            val layoutPendingDistribution =
                getDrawable(context.resources, R.drawable.layout_thin_border_status_pending_distribution, null)
            val layoutProcessed = getDrawable(context.resources, R.drawable.layout_thin_border_status_processed, null)
            val layoutNoStatus = getDrawable(context.resources, R.drawable.layout_thin_border_status_no_status, null)
            val layoutActive = getDrawable(context.resources, R.drawable.layout_thin_border_status_active, null)
            val layoutDeactivated =
                getDrawable(context.resources, R.drawable.layout_thin_border_status_deactivated, null)
            val layoutFlashing = getDrawable(context.resources, R.drawable.layout_thin_border_status_flashing, null)

            val backColor: Drawable
            val foreColor: Int

            when (item.status) {
                OrderStatus.approved -> {
                    backColor = layoutApproved!!
                    foreColor = approvedForeColor
                }

                OrderStatus.active -> {
                    backColor = layoutActive!!
                    foreColor = activeForeColor
                }

                OrderStatus.draft -> {
                    backColor = layoutDraft!!
                    foreColor = draftForeColor
                }

                OrderStatus.deactivated -> {
                    backColor = layoutDeactivated!!
                    foreColor = deactivatedForeColor
                }

                OrderStatus.inProcess -> {
                    backColor = layoutInProcess!!
                    foreColor = inProcessForeColor
                }

                OrderStatus.inTransit -> {
                    backColor = layoutInTransit!!
                    foreColor = inTransitForeColor
                }

                OrderStatus.delivered -> {
                    backColor = layoutDelivered!!
                    foreColor = deliveredForeColor
                }

                OrderStatus.finished -> {
                    backColor = layoutFinished!!
                    foreColor = finishedForeColor
                }

                OrderStatus.pending -> {
                    backColor = layoutPending!!
                    foreColor = pendingForeColor
                }

                OrderStatus.pendingDistribution -> {
                    backColor = layoutPendingDistribution!!
                    foreColor = pendingDistributionForeColor
                }

                OrderStatus.processed -> {
                    backColor = layoutProcessed!!
                    foreColor = processedForeColor
                }

                OrderStatus.flashing -> {
                    backColor = layoutFlashing!!
                    foreColor = flashingForeColor
                }

                else -> {
                    backColor = layoutNoStatus!!
                    foreColor = noStatusForeColor
                }
            }

            v.background = backColor
            binding.extIdTextView.setTextColor(foreColor)
            binding.descriptionTextView.setTextColor(foreColor)
            binding.creationDateTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)
        }
    }

    companion object {
        private object ItemDiffUtilCallback : DiffUtil.ItemCallback<OrderResponse>() {
            override fun areItemsTheSame(oldItem: OrderResponse, newItem: OrderResponse): Boolean {
                return oldItem.hashCode == newItem.hashCode
            }

            override fun areContentsTheSame(oldItem: OrderResponse, newItem: OrderResponse): Boolean {
                if (oldItem.id != newItem.id) return false
                return oldItem.hashCode == newItem.hashCode
            }
        }

        // Aquí definimos dos constantes para identificar los dos diseños diferentes
        const val SELECTED_VIEW_TYPE = 1
        const val UNSELECTED_VIEW_TYPE = 2

        // region COLORS
        private var approvedForeColor: Int = 0
        private var draftForeColor: Int = 0
        private var inProcessForeColor: Int = 0
        private var inTransitForeColor: Int = 0
        private var deliveredForeColor: Int = 0
        private var finishedForeColor: Int = 0
        private var pendingForeColor: Int = 0
        private var pendingDistributionForeColor: Int = 0
        private var processedForeColor: Int = 0
        private var noStatusForeColor: Int = 0
        private var activeForeColor: Int = 0
        private var deactivatedForeColor: Int = 0
        private var flashingForeColor: Int = 0

        private var approvedSelectedForeColor: Int = 0
        private var draftSelectedForeColor: Int = 0
        private var inProcessSelectedForeColor: Int = 0
        private var inTransitSelectedForeColor: Int = 0
        private var deliveredSelectedForeColor: Int = 0
        private var finishedSelectedForeColor: Int = 0
        private var pendingSelectedForeColor: Int = 0
        private var pendingDistributionSelectedForeColor: Int = 0
        private var processedSelectedForeColor: Int = 0
        private var noStatusSelectedForeColor: Int = 0
        private var activeSelectedForeColor: Int = 0
        private var deactivatedSelectedForeColor: Int = 0
        private var flashingSelectedForeColor: Int = 0

        private var darkslategray: Int = 0
        private var lightgray: Int = 0

        /**
         * Setup colors
         * Simplemente inicializamos algunas variables con los colores que vamos a usar para cada estado.
         */
        private fun setupColors() {
            // Color de los diferentes estados
            val statusApproved = getColor(context.resources, R.color.status_approved, null)
            val statusDraft = getColor(context.resources, R.color.status_draft, null)
            val statusInProcess = getColor(context.resources, R.color.status_in_process, null)
            val statusInTransit = getColor(context.resources, R.color.status_in_transit, null)
            val statusDelivered = getColor(context.resources, R.color.status_delivered, null)
            val statusFinished = getColor(context.resources, R.color.status_finished, null)
            val statusPending = getColor(context.resources, R.color.status_pending, null)
            val statusPendingDistribution = getColor(context.resources, R.color.status_pending_distribution, null)
            val statusProcessed = getColor(context.resources, R.color.status_processed, null)
            val statusNoStatus = getColor(context.resources, R.color.status_no_status, null)
            val statusActive = getColor(context.resources, R.color.status_active, null)
            val statusDeactivated = getColor(context.resources, R.color.status_deactivated, null)
            val statusFlashing = getColor(context.resources, R.color.status_flashing, null)

            // Mejor contraste para los ítems seleccionados
            approvedSelectedForeColor = getBestContrastColor(manipulateColor(statusApproved, 0.5f))
            draftSelectedForeColor = getBestContrastColor(manipulateColor(statusDraft, 0.5f))
            inProcessSelectedForeColor = getBestContrastColor(manipulateColor(statusInProcess, 0.5f))
            inTransitSelectedForeColor = getBestContrastColor(manipulateColor(statusInTransit, 0.5f))
            deliveredSelectedForeColor = getBestContrastColor(manipulateColor(statusDelivered, 0.5f))
            finishedSelectedForeColor = getBestContrastColor(manipulateColor(statusFinished, 0.5f))
            pendingSelectedForeColor = getBestContrastColor(manipulateColor(statusPending, 0.5f))
            pendingDistributionSelectedForeColor =
                getBestContrastColor(manipulateColor(statusPendingDistribution, 0.5f))
            processedSelectedForeColor = getBestContrastColor(manipulateColor(statusProcessed, 0.5f))
            noStatusSelectedForeColor = getBestContrastColor(manipulateColor(statusNoStatus, 0.5f))
            activeSelectedForeColor = getBestContrastColor(manipulateColor(statusActive, 0.5f))
            deactivatedSelectedForeColor = getBestContrastColor(manipulateColor(statusDeactivated, 0.5f))
            flashingSelectedForeColor = getBestContrastColor(manipulateColor(statusFlashing, 0.5f))

            // Mejor contraste para los ítems no seleccionados
            approvedForeColor = getBestContrastColor(statusApproved)
            draftForeColor = getBestContrastColor(statusDraft)
            inProcessForeColor = getBestContrastColor(statusInProcess)
            inTransitForeColor = getBestContrastColor(statusInTransit)
            deliveredForeColor = getBestContrastColor(statusDelivered)
            finishedForeColor = getBestContrastColor(statusFinished)
            pendingForeColor = getBestContrastColor(statusPending)
            pendingDistributionForeColor = getBestContrastColor(statusPendingDistribution)
            processedForeColor = getBestContrastColor(statusProcessed)
            noStatusForeColor = getBestContrastColor(statusNoStatus)
            activeForeColor = getBestContrastColor(statusActive)
            deactivatedForeColor = getBestContrastColor(statusDeactivated)
            flashingForeColor = getBestContrastColor(statusFlashing)

            // CheckBox color
            darkslategray = getColor(context.resources, R.color.darkslategray, null)

            // Title color
            lightgray = getColor(context.resources, R.color.lightgray, null)
        }
        // endregion
    }

    init {
        selectedItemChangedListener = builder.selectedItemChangedListener

        setupColors()
    }

    class Builder {
        fun build(): OrderPagingAdapter {
            return OrderPagingAdapter(this)
        }

        internal var selectedItemChangedListener: SelectedItemChangedListener? = null

        @Suppress("unused")
        fun selectedItemChangedListener(listener: SelectedItemChangedListener?): Builder {
            selectedItemChangedListener = listener
            return this
        }
    }
}
