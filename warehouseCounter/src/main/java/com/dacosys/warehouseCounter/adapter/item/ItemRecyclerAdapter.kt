package com.dacosys.warehouseCounter.adapter.item

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.*
import com.dacosys.imageControl.adapter.ImageAdapter
import com.dacosys.imageControl.adapter.ImageAdapter.Companion.GetImageStatus
import com.dacosys.imageControl.adapter.ImageAdapter.Companion.ImageControlHolder
import com.dacosys.imageControl.network.common.ProgramData
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.adapter.ptlOrder.PtlOrderAdapter.Companion.FilterOptions
import com.dacosys.warehouseCounter.databinding.ItemRowBinding
import com.dacosys.warehouseCounter.databinding.ItemRowExpandedBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.table.Table
import com.dacosys.warehouseCounter.room.entity.item.Item
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import io.ktor.util.reflect.*
import java.util.*

class ItemRecyclerAdapter(
    private val recyclerView: RecyclerView,
    var fullList: ArrayList<Item> = ArrayList(),
    var checkedIdArray: ArrayList<Long> = ArrayList(),
    private var multiSelect: Boolean = false,
    var showCheckBoxes: Boolean = false,
    private var showCheckBoxesChanged: (Boolean) -> Unit = { },
    private var showImages: Boolean = false,
    private var showImagesChanged: (Boolean) -> Unit = { },
    private var visibleStatus: ArrayList<ItemStatus> = ArrayList(ItemStatus.values().toList()),
    private var filterOptions: FilterOptions = FilterOptions("", true)
) : ListAdapter<Item, ViewHolder>(ItemDiffUtilCallback), Filterable {

    // Posición del ítem seleccionado
    private var currentIndex = NO_POSITION

    // Este Listener debe usarse para los cambios de cantidad o de ítems marcados de la lista,
    // ya que se utiliza para actualizar los valores sumarios en la actividad.
    private var dataSetChangedListener: DataSetChangedListener? = null

    private var selectedItemChangedListener: SelectedItemChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null
    private var editItemRequiredListener: EditItemRequiredListener? = null

    // Listeners para los eventos de ImageControl.
    private var addPhotoRequiredListener: AddPhotoRequiredListener? = null
    private var albumViewRequiredListener: AlbumViewRequiredListener? = null

    // Clase para distinguir actualizaciones parciales
    private enum class PAYLOADS {
        CHECKBOX_STATE,
        CHECKBOX_VISIBILITY,
        IMAGE_VISIBILITY,
        IMAGE_CONTROL_VISIBILITY,
        ITEM_SELECTED
    }

    /**
     * Show an images panel on the end of layout
     *
     * @param show
     */
    fun showImages(show: Boolean) {
        showImages = show
        showImagesChanged.invoke(showImages)
        notifyItemRangeChanged(0, itemCount, PAYLOADS.IMAGE_VISIBILITY)
    }

    /**
     * Change image control panel visibility on the bottom of the layout.
     * The state is defined by [settingViewModel.useImageControl] preference property.
     *
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun showImageControlPanel() {
        notifyItemRangeChanged(currentIndex, 1, PAYLOADS.IMAGE_CONTROL_VISIBILITY)
    }

    // Ids de ítems que tienen imágenes
    private var idWithImage: ArrayList<Long> = ArrayList()

    // Visibilidad del panel de miniaturas depende de la existencia de una imagen para ese ítem.
    private fun imageVisibility(itemId: Long): Int {
        return if (showImages && idWithImage.contains(itemId)) VISIBLE else GONE
    }

    // Permiso de edición de ítems
    private val userHasPermissionToEdit: Boolean by lazy { settingViewModel.editItems }

    fun clear() {
        checkedIdArray.clear()
        idWithImage.clear()

        fullList.clear()
        submitList(fullList)
    }

    fun refreshListeners(
        checkedChangedListener: CheckedChangedListener? = null,
        dataSetChangedListener: DataSetChangedListener? = null,
        editItemRequiredListener: EditItemRequiredListener? = null,
        selectedItemChangedListener: SelectedItemChangedListener? = null,
    ) {
        this.checkedChangedListener = checkedChangedListener
        this.dataSetChangedListener = dataSetChangedListener
        this.editItemRequiredListener = editItemRequiredListener
        this.selectedItemChangedListener = selectedItemChangedListener
    }

    fun refreshImageControlListeners(
        addPhotoListener: AddPhotoRequiredListener? = null,
        albumViewListener: AlbumViewRequiredListener? = null,
    ) {
        addPhotoRequiredListener = addPhotoListener
        albumViewRequiredListener = albumViewListener
    }

    interface DataSetChangedListener {
        fun onDataSetChanged()
    }

    interface CheckedChangedListener {
        fun onCheckedChanged(isChecked: Boolean, pos: Int)
    }

    interface SelectedItemChangedListener {
        fun onSelectedItemChanged(item: Item?)
    }

    interface AlbumViewRequiredListener {
        fun onAlbumViewRequired(tableId: Int, itemId: Long)
    }

    interface EditItemRequiredListener {
        fun onEditItemRequired(tableId: Int, itemId: Long)
    }

    interface AddPhotoRequiredListener {
        fun onAddPhotoRequired(tableId: Int, itemId: Long, description: String)
    }

    // El método onCreateViewHolder infla los diseños para cada tipo de vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            SELECTED_VIEW_TYPE -> {
                SelectedViewHolder(ItemRowExpandedBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            else -> {
                UnselectedViewHolder(ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }
    }

    // Sobrecarga del método onBindViewHolder para actualización parcial de las vistas
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            // No hay payload, realizar la vinculación completa
            super.onBindViewHolder(holder, position, payloads)
            return
        }

        // Hay payload, realizar la actualización parcial basada en el payload
        for (payload in payloads) {
            // Extraer el payload y utilizarlo para actualizar solo las vistas relevantes
            when (payload) {
                PAYLOADS.CHECKBOX_VISIBILITY -> {
                    if (position == currentIndex)
                        (holder as SelectedViewHolder).bindCheckBoxVisibility(if (showCheckBoxes) VISIBLE else GONE)
                    else
                        (holder as UnselectedViewHolder).bindCheckBoxVisibility(if (showCheckBoxes) VISIBLE else GONE)
                }

                PAYLOADS.CHECKBOX_STATE -> {
                    val item = getItem(position)
                    if (position == currentIndex)
                        (holder as SelectedViewHolder).bindCheckBoxState(checkedIdArray.contains(item.itemId))
                    else
                        (holder as UnselectedViewHolder).bindCheckBoxState(checkedIdArray.contains(item.itemId))
                }

                PAYLOADS.IMAGE_VISIBILITY -> {
                    val item = getItem(position)
                    if (position == currentIndex) {
                        getImagesThumbs(this, (holder as SelectedViewHolder).icHolder, item)
                        holder.bindImageVisibility(
                            imageVisibility = imageVisibility(item.itemId),
                            changingState = true
                        )
                    } else {
                        getImagesThumbs(this, (holder as UnselectedViewHolder).icHolder, item)
                        holder.bindImageVisibility(
                            imageVisibility = imageVisibility(item.itemId),
                            changingState = true
                        )
                    }
                }

                PAYLOADS.IMAGE_CONTROL_VISIBILITY -> {
                    if (position == currentIndex) {
                        (holder as SelectedViewHolder).bindImageControlVisibility(if (settingViewModel.useImageControl) VISIBLE else GONE)
                    }
                    if (!settingViewModel.useImageControl) showImages(false)
                }

                PAYLOADS.ITEM_SELECTED -> {
                    // TODO: No regenerar la vista ante cambios de selección
                    // No está funcionando. La idea es usarlo para los cambios de selección.
                    // Pero por algún motivo los Payloads vienen vacíos luego de notifyItemChanged
                    super.onBindViewHolder(holder, position, payloads)
                }
            }
        }
    }

    // El método onBindViewHolder establece los valores de las vistas en función de los datos
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Evento de clic sobre el ítem que controla el estado de Selección para seleccionar el diseño adecuado
        holder.itemView.setOnClickListener {

            // Si el elemento ya está seleccionado quitar la selección
            if (currentIndex == holder.bindingAdapterPosition) {
                currentIndex = NO_POSITION
                notifyItemSelectedChanged(holder.bindingAdapterPosition)
            } else {
                // Notificamos los cambios para los dos ítems cuyo diseño necesita cambiar
                val previousSelectedItemPosition = currentIndex
                currentIndex = holder.bindingAdapterPosition
                notifyItemSelectedChanged(currentIndex)

                if (previousSelectedItemPosition != NO_POSITION) {
                    notifyItemSelectedChanged(previousSelectedItemPosition)
                }

                // Scroll para asegurarnos que se vea completamente el ítem
                holder.itemView.post { scrollToPos(currentIndex) }
            }

            // Seleccionamos el ítem
            holder.itemView.isSelected = currentIndex == position

            // Notificamos al Listener superior
            dataSetChangedListener?.onDataSetChanged()
        }

        // Establecer los valores para cada elemento según su posición con el estilo correspondiente
        if (currentIndex == position) {
            // Establece el estado seleccionado
            setSelectedHolder(holder as SelectedViewHolder, position)
        } else {
            // Establece el estado no seleccionado
            setUnselectedHolder(holder as UnselectedViewHolder, position)
        }
    }

    private fun setSelectedHolder(holder: SelectedViewHolder, position: Int) {
        val item = getItem(position)

        // Lógica de clic largo sobre el ítem
        setItemCheckBoxLogic(holder.itemView)

        holder.bind(
            item = item,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE,
            imageVisibility = imageVisibility(item.itemId)
        )

        holder.itemView.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            getColorWithAlpha(colorId = R.color.lightslategray, alpha = 220), BlendModeCompat.MODULATE
        )

        // Acciones del checkBox de marcado
        setCheckBoxLogic(holder.binding.checkBox, item, position)

        // Acción de edición
        setEditLogic(holder.binding.editImageView, item)

        // Botones de acciones de ImageControl
        setAddSignLogic(holder.binding.signImageView)
        setAlbumViewLogic(holder.binding.albumImageView, item)
        setAddPhotoLogic(holder.binding.addPhotoImageView, item)

        // Miniatura de ImageControl
        getImagesThumbs(this, holder.icHolder, item)
    }

    private fun setUnselectedHolder(holder: UnselectedViewHolder, position: Int) {
        val item = getItem(position)

        // Lógica de clic largo sobre el ítem
        setItemCheckBoxLogic(holder.itemView)

        // Perform a full update
        holder.bind(
            item = item,
            checkBoxVisibility = if (showCheckBoxes) VISIBLE else GONE,
            imageVisibility = imageVisibility(item.itemId)
        )

        holder.itemView.background.colorFilter = null

        // Acciones del checkBox de marcado
        setCheckBoxLogic(holder.binding.checkBox, item, position)

        // Miniatura de ImageControl
        getImagesThumbs(this, holder.icHolder, item)
    }

    private fun setAddSignLogic(signImageView: AppCompatImageView) {
        signImageView.visibility = GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setAlbumViewLogic(albumImageView: AppCompatImageView, item: Item) {
        albumImageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                albumViewRequiredListener?.onAlbumViewRequired(
                    tableId = Table.item.tableId, itemId = item.itemId
                )
            }
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setAddPhotoLogic(addPhotoImageView: AppCompatImageView, item: Item) {
        addPhotoImageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                addPhotoRequiredListener?.onAddPhotoRequired(
                    tableId = Table.item.tableId, itemId = item.itemId, description = item.description
                )
            }
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setEditLogic(editImageView: AppCompatImageView, item: Item) {
        if (!userHasPermissionToEdit) {
            editImageView.visibility = GONE
            return
        }

        editImageView.visibility = VISIBLE

        editImageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                editItemRequiredListener?.onEditItemRequired(
                    tableId = Table.item.tableId, itemId = item.itemId
                )
            }
            true
        }
    }

    /**
     * Lógica del evento de clic sostenido sobre el ítem que cambia la visibilidad de los CheckBox
     *
     * @param itemView Vista general del ítem
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setItemCheckBoxLogic(itemView: View) {
        if (!multiSelect) {
            showCheckBoxes = false
            return
        }

        val longClickListener = OnLongClickListener { _ ->
            showCheckBoxes = !showCheckBoxes
            showCheckBoxesChanged.invoke(showCheckBoxes)
            notifyItemRangeChanged(0, itemCount, PAYLOADS.CHECKBOX_VISIBILITY)
            return@OnLongClickListener true
        }

        itemView.isLongClickable = true
        itemView.setOnLongClickListener(longClickListener)
    }

    /**
     * Lógica del comportamiento del CheckBox de marcado de ítems cuando [multiSelect] es verdadero
     *
     * @param checkBox Control CheckBox para marcado del ítem
     * @param item Datos del ítem
     * @param position Posición en el adaptador
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCheckBoxLogic(checkBox: CheckBox, item: Item, position: Int) {
        val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            setChecked(item = item, isChecked = isChecked, suspendRefresh = true)
        }

        val longClickListener = OnLongClickListener { _ ->
            checkBox.setOnCheckedChangeListener(null)

            // Notificamos los cambios solo a los ítems que cambian de estado.
            val newState = !checkBox.isChecked
            if (newState) {
                currentList.mapIndexed { pos, item ->
                    if (item.itemId !in checkedIdArray) {
                        checkedIdArray.add(item.itemId)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            } else {
                currentList.mapIndexed { pos, item ->
                    if (item.itemId in checkedIdArray) {
                        checkedIdArray.remove(item.itemId)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            }

            // Notificamos al Listener superior
            dataSetChangedListener?.onDataSetChanged()
            return@OnLongClickListener true
        }

        // Important to remove previous checkedChangedListener before calling setChecked
        checkBox.setOnCheckedChangeListener(null)

        checkBox.isChecked = checkedIdArray.contains(item.itemId)
        checkBox.isLongClickable = true
        checkBox.tag = position

        checkBox.setOnLongClickListener(longClickListener)
        checkBox.setOnCheckedChangeListener(checkChangeListener)
    }

    // El método getItemViewType devuelve el tipo de vista que se usará para el elemento en la posición dada
    override fun getItemViewType(position: Int): Int {
        return if (currentIndex == position) SELECTED_VIEW_TYPE
        else UNSELECTED_VIEW_TYPE
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            var selected: Item? = null
            var firstVisible: Item? = null

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                // Guardamos el item seleccionado y la posición del scroll
                selected = currentItem()
                var scrollPos =
                    (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition()
                        ?: NO_POSITION

                if (scrollPos != NO_POSITION && itemCount > scrollPos) {
                    var currentScrolled = getItem(scrollPos)

                    // Comprobamos si es visible el ítem del Scroll
                    if (currentScrolled.itemStatus in visibleStatus)
                        firstVisible = currentScrolled
                    else {
                        // Si no es visible, intentar encontrar el próximo visible.
                        while (firstVisible == null) {
                            scrollPos++
                            if (itemCount > scrollPos) {
                                currentScrolled = getItem(scrollPos)
                                if (currentScrolled.itemStatus in visibleStatus)
                                    firstVisible = currentScrolled
                            } else break
                        }
                    }
                }

                // Filtramos los resultados
                val results = FilterResults()
                var r: ArrayList<Item> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableItem: Item

                        for (i in 0 until fullList.size) {
                            filterableItem = fullList[i]

                            // Descartamos aquellos que no debe ser visibles
                            if (filterableItem.itemStatus !in visibleStatus) continue

                            if (isFilterable(filterableItem, filterString)) {
                                r.add(filterableItem)
                            }
                        }
                    } else if (filterOptions.showAllOnFilterEmpty) {
                        r = ArrayList(fullList.mapNotNull { if (it.itemStatus in visibleStatus) it else null })
                    }
                }

                results.values = r
                results.count = r.size
                return results
            }

            fun isFilterable(filterableItem: Item, filterString: String): Boolean =
                (filterableItem.description.contains(filterString, true) ||
                        filterableItem.ean.contains(filterString, true))

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                submitList(results?.values as ArrayList<Item>) {
                    run {
                        // Notificamos al Listener superior
                        dataSetChangedListener?.onDataSetChanged()

                        // Recuperamos el item seleccionado y la posición del scroll
                        if (firstVisible != null)
                            scrollToPos(getIndexById(firstVisible?.itemId ?: -1), true)
                        if (selected != null)
                            selectItem(selected, false)
                    }
                }
            }
        }
    }

    private fun sortItems(originalList: MutableList<Item>): ArrayList<Item> {
        // Run the follow method on each of the roots
        return ArrayList(
            originalList.sortedWith(
                compareBy({ it.description },
                    { it.ean })
            ).toList()
        )
    }

    fun refreshFilter(options: FilterOptions) {
        filterOptions = options
        filter.filter(filterOptions.filterString)
    }

    private fun refreshFilter() {
        refreshFilter(filterOptions)
    }

    fun add(item: Item, position: Int) {
        fullList.add(position, item)
        submitList(fullList) {
            run {
                // Notificamos al Listener superior
                dataSetChangedListener?.onDataSetChanged()

                selectItem(position)
            }
        }
    }

    fun remove(position: Int) {
        val id = getItemId(position)
        checkedIdArray.remove(id)

        fullList.removeAt(position)
        submitList(fullList) {
            run {
                // Notificamos al Listener superior
                dataSetChangedListener?.onDataSetChanged()
            }
        }
    }

    /**
     * Se utiliza cuando se edita un ítem y necesita actualizarse
     */
    fun updateItem(item: Item, scrollToPos: Boolean = false) {
        val t = fullList.firstOrNull { it == item } ?: return

        t.itemId = item.itemId
        t.description = item.description
        t.active = item.active
        t.price = item.price
        t.ean = item.ean
        t.itemCategoryId = item.itemCategoryId
        t.externalId = item.externalId
        t.lotEnabled = item.lotEnabled
        t.itemCategoryStr = item.itemCategoryStr

        submitList(fullList) {
            run {
                // Notificamos al Listener superior
                dataSetChangedListener?.onDataSetChanged()

                // Seleccionamos el ítem y hacemos scroll hasta él.
                selectItem(item, scrollToPos)
            }
        }
    }

    fun selectItem(a: Item?, scroll: Boolean = true) {
        var pos = NO_POSITION
        if (a != null) pos = getIndex(a)
        selectItem(pos, scroll)
    }

    fun selectItem(pos: Int, scroll: Boolean = true) {
        // Si la posición está fuera del rango válido, reseteamos currentIndex a NO_POSITION.
        currentIndex = if (pos < 0 || pos >= itemCount) NO_POSITION else pos
        notifyItemSelectedChanged(currentIndex)
        if (scroll) scrollToPos(currentIndex)
    }

    private fun notifyItemSelectedChanged(pos: Int) {
        notifyItemChanged(currentIndex)
        var item: Item? = null
        if (pos != NO_POSITION) item = getItem(pos)
        selectedItemChangedListener?.onSelectedItemChanged(item)
    }

    /**
     * Scrolls to the given position, making sure the item can be fully displayed.
     *
     * @param position
     * @param scrollToTop If it is activated, the item will scroll until it is at the top of the view
     */
    fun scrollToPos(position: Int, scrollToTop: Boolean = false) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager

        if (position < 0 || position >= itemCount) {
            // La posición está fuera del rango válido, no se puede realizar el scroll
            return
        }

        val selectedView = layoutManager.findViewByPosition(position)

        if (scrollToTop) {
            // Hacemos scroll hasta que el ítem quede en la parte superior
            layoutManager.scrollToPositionWithOffset(position, 0)
        } else {
            if (selectedView != null) {
                // El ítem es visible, realizar scroll para asegurarse de que se vea completamente
                scrollToVisibleItem(selectedView)
            } else {
                // El ítem no es visible, realizar scroll directo a la posición
                recyclerView.scrollToPosition(position)
                recyclerView.addOnScrollListener(object : OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val selected = layoutManager.findViewByPosition(position)
                        if (selected != null) {
                            // El ítem se ha vuelto visible, realizar scroll para asegurarse de que se vea completamente
                            scrollToVisibleItem(selected)
                            recyclerView.removeOnScrollListener(this)
                        }
                    }
                })
            }
        }
    }

    /**
     * Scrolls to the given view, making sure the item can be fully displayed.
     *
     * @param selectedView
     */
    private fun scrollToVisibleItem(selectedView: View) {
        val recyclerViewHeight = recyclerView.height - recyclerView.paddingTop - recyclerView.paddingBottom
        val selectedViewHeight = selectedView.height + selectedView.marginTop + selectedView.marginBottom

        val selectedViewTop = selectedView.top - selectedView.marginTop
        val selectedViewBottom = selectedView.bottom + selectedView.marginBottom

        if (selectedViewTop < 0) {
            // El ítem está parcialmente oculto en la parte superior del RecyclerView
            recyclerView.smoothScrollBy(0, selectedViewTop)
        } else if (selectedViewBottom > recyclerViewHeight) {
            // El ítem está parcialmente oculto en la parte inferior del RecyclerView
            val visibleHeight = recyclerViewHeight - selectedViewTop
            recyclerView.smoothScrollBy(0, selectedViewHeight - visibleHeight)
        }
    }

    fun getIndexById(id: Long): Int {
        return currentList.indexOfFirst { it.itemId == id }
    }

    private fun getIndex(item: Item): Int {
        return currentList.indexOfFirst { it == item }
    }

    private fun getItemById(id: Long): Item? {
        return fullList.firstOrNull { it.itemId == id }
    }

    fun getAllChecked(): ArrayList<Item> {
        val items = ArrayList<Item>()
        checkedIdArray.mapNotNullTo(items) { getItemById(it) }
        return items
    }

    fun currentItem(): Item? {
        if (currentIndex == NO_POSITION) return null
        return if (currentList.any() && itemCount > currentIndex) getItem(currentIndex)
        else null
    }

    fun countChecked(): Int {
        return checkedIdArray.size
    }

    fun totalVisible(): Int {
        return itemCount
    }

    fun firstVisiblePos(): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        return layoutManager.findFirstVisibleItemPosition()
    }

    fun setChecked(item: Item, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val pos = getIndexById(item.itemId)
        if (isChecked) {
            if (!checkedIdArray.contains(item.itemId)) {
                checkedIdArray.add(item.itemId)
            }
        } else {
            checkedIdArray.remove(item.itemId)
        }

        checkedChangedListener?.onCheckedChanged(isChecked, pos)

        // Notificamos al Listener superior
        if (!suspendRefresh) dataSetChangedListener?.onDataSetChanged()
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<Item>, isChecked: Boolean) {
        if (isFilling) return

        isFilling = true
        for (i in items) {
            setChecked(item = i, isChecked = isChecked, suspendRefresh = true)
        }
        isFilling = false

        // Notificamos al Listener superior
        dataSetChangedListener?.onDataSetChanged()
    }

    fun addVisibleStatus(status: ItemStatus) {
        if (visibleStatus.contains(status)) return
        visibleStatus.add(status)

        refreshFilter()
    }

    fun removeVisibleStatus(status: ItemStatus) {
        if (!visibleStatus.contains(status)) return
        visibleStatus.remove(status)

        // Quitamos los ítems con el estado seleccionado de la lista marcados.
        val uncheckedItems = ArrayList(fullList.mapNotNull { if (it.itemStatus == status) it.itemId else null })
        checkedIdArray.removeAll(uncheckedItems.toSet())

        refreshFilter()
    }

    internal class SelectedViewHolder(val binding: ItemRowExpandedBinding) :
        ViewHolder(binding.root) {
        val icHolder: ImageControlHolder = ImageControlHolder().apply {
            imageConstraintLayout = binding.imageConstraintLayout
            imageImageView = binding.imageView
            progressBar = binding.progressBar
        }

        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        /**
         * Bind image visibility
         *
         * @param imageVisibility Visibility of the image panel
         * @param changingState Only if we are changing the visibility state, we expand the panel
         */
        fun bindImageVisibility(imageVisibility: Int, changingState: Boolean) {
            if (!settingViewModel.useImageControl || imageVisibility == GONE) collapseImagePanel(icHolder)
            else if (changingState) expandImagePanel(icHolder)
        }

        /**
         * Bind image control visibility
         *
         * @param visibility Visibility of the image control panel
         */
        fun bindImageControlVisibility(visibility: Int = GONE) {
            binding.imageControlConstraintLayout.visibility = visibility
        }

        fun bind(item: Item, checkBoxVisibility: Int = GONE, imageVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)
            bindImageControlVisibility(visibility = if (settingViewModel.useImageControl) VISIBLE else GONE)
            bindImageVisibility(imageVisibility = imageVisibility, changingState = false)

            binding.descriptionTextView.text = item.description
            binding.eanTextView.text = item.ean
            binding.priceCheckedTextView.text = String.format("$ %s", Statics.roundToString(item.price ?: 0F, 2))
            binding.extIdCheckedTextView.text = item.externalId ?: 0.toString()

            // region Category
            val category = item.itemCategoryStr
            if (category.isEmpty()) {
                binding.categoryConstraintLayout.visibility = GONE
            } else {
                binding.categoryConstraintLayout.visibility = VISIBLE
                binding.categoryTextView.text = category
            }

            setStyle(item)
        }

        private fun setStyle(item: Item) {
            val v = itemView

            // Background layouts
            // Resalta por estado del ítem
            val layoutLotActive = getDrawable(context.resources, R.drawable.layout_thin_border, null)
            val layoutNotLotActive = getDrawable(context.resources, R.drawable.layout_thin_border_light_blue, null)
            val layoutLotInactive = getDrawable(context.resources, R.drawable.layout_thin_border_gray, null)
            val layoutNotLotInactive = getDrawable(context.resources, R.drawable.layout_thin_border_light_blue_2, null)

            val backColor: Drawable
            val foreColor: Int

            when (item.itemStatus) {
                ItemStatus.ACTIVE_LOT_ENABLED -> {
                    backColor = layoutLotActive!!
                    foreColor = lotActiveSelectedForeColor
                }

                ItemStatus.ACTIVE_LOT_DISABLED -> {
                    backColor = layoutNotLotActive!!
                    foreColor = notLotActiveSelectedForeColor
                }

                ItemStatus.INACTIVE_LOT_ENABLED -> {
                    backColor = layoutLotInactive!!
                    foreColor = lotInactiveSelectedForeColor
                }

                ItemStatus.INACTIVE_LOT_DISABLED -> {
                    backColor = layoutNotLotInactive!!
                    foreColor = notLotInactiveSelectedForeColor
                }
            }

            val titleForeColor: Int = manipulateColor(foreColor, 0.8f)

            v.background = backColor
            binding.descriptionTextView.setTextColor(foreColor)
            binding.eanTextView.setTextColor(foreColor)
            binding.categoryTextView.setTextColor(foreColor)
            binding.priceCheckedTextView.setTextColor(foreColor)
            binding.extIdCheckedTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)

            // Titles
            binding.categoryLabelTextView.setTextColor(titleForeColor)
            binding.priceLabelTextView.setTextColor(titleForeColor)
            binding.extIdLabelTextView.setTextColor(titleForeColor)
        }
    }

    internal class UnselectedViewHolder(val binding: ItemRowBinding) :
        ViewHolder(binding.root) {
        val icHolder: ImageControlHolder = ImageControlHolder().apply {
            imageConstraintLayout = binding.imageConstraintLayout
            imageImageView = binding.imageView
            progressBar = binding.progressBar
        }

        fun bindCheckBoxVisibility(checkBoxVisibility: Int = GONE) {
            binding.checkBoxConstraintLayout.visibility = checkBoxVisibility
        }

        fun bindCheckBoxState(checked: Boolean) {
            binding.checkBox.isChecked = checked
        }

        /**
         * Bind image visibility
         *
         * @param imageVisibility Visibility of the image panel
         * @param changingState Only if we are changing the visibility state, we expand the panel
         */
        fun bindImageVisibility(imageVisibility: Int, changingState: Boolean) {
            if (!settingViewModel.useImageControl || imageVisibility == GONE) collapseImagePanel(icHolder)
            else if (changingState) expandImagePanel(icHolder)
        }

        fun bind(item: Item, checkBoxVisibility: Int = GONE, imageVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)
            bindImageVisibility(imageVisibility = imageVisibility, changingState = false)

            binding.descriptionTextView.text = item.description
            binding.eanTextView.text = item.ean

            setStyle(item)
        }

        private fun setStyle(item: Item) {
            val v = itemView

            // Background layouts
            // Resalta por estado del ítem
            val layoutLotActive = getDrawable(context.resources, R.drawable.layout_thin_border, null)
            val layoutNotLotActive = getDrawable(context.resources, R.drawable.layout_thin_border_light_blue, null)
            val layoutLotInactive = getDrawable(context.resources, R.drawable.layout_thin_border_gray, null)
            val layoutNotLotInactive = getDrawable(context.resources, R.drawable.layout_thin_border_light_blue_2, null)

            val backColor: Drawable
            val foreColor: Int
            val titleForeColor: Int = darkslategray

            when (item.itemStatus) {
                ItemStatus.ACTIVE_LOT_ENABLED -> {
                    backColor = layoutLotActive!!
                    foreColor = lotActiveForeColor
                }

                ItemStatus.ACTIVE_LOT_DISABLED -> {
                    backColor = layoutNotLotActive!!
                    foreColor = notLotActiveForeColor
                }

                ItemStatus.INACTIVE_LOT_ENABLED -> {
                    backColor = layoutLotInactive!!
                    foreColor = lotInactiveForeColor
                }

                ItemStatus.INACTIVE_LOT_DISABLED -> {
                    backColor = layoutNotLotInactive!!
                    foreColor = notLotInactiveForeColor
                }
            }

            v.background = backColor
            binding.descriptionTextView.setTextColor(foreColor)
            binding.eanTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)
        }
    }

    private object ItemDiffUtilCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            if (oldItem.itemId != newItem.itemId) return false
            if (oldItem.description != newItem.description) return false
            if (oldItem.active != newItem.active) return false
            if (oldItem.price != newItem.price) return false
            if (oldItem.ean != newItem.ean) return false
            if (oldItem.itemCategoryId != newItem.itemCategoryId) return false
            if (oldItem.externalId != newItem.externalId) return false
            if (oldItem.lotEnabled != newItem.lotEnabled) return false
            return oldItem.itemCategoryStr == newItem.itemCategoryStr
        }
    }

    companion object {
        fun collapseImagePanel(icHolder: ImageControlHolder) {
            icHolder.imageConstraintLayout?.post { icHolder.imageConstraintLayout?.visibility = GONE }
            icHolder.imageImageView?.post { icHolder.imageImageView?.visibility = INVISIBLE }
            icHolder.progressBar?.post { icHolder.progressBar?.visibility = GONE }
        }

        private fun waitingImagePanel(icHolder: ImageControlHolder) {
            icHolder.imageImageView?.post { icHolder.imageImageView?.visibility = INVISIBLE }
            icHolder.progressBar?.post { icHolder.progressBar?.visibility = VISIBLE }
            icHolder.imageConstraintLayout?.post { icHolder.imageConstraintLayout?.visibility = VISIBLE }
        }

        private fun expandImagePanel(icHolder: ImageControlHolder) {
            icHolder.imageImageView?.post { icHolder.imageImageView?.visibility = VISIBLE }
            icHolder.progressBar?.post { icHolder.progressBar?.visibility = GONE }
            icHolder.imageConstraintLayout?.post { icHolder.imageConstraintLayout?.visibility = VISIBLE }
        }

        private fun showImagePanel(icHolder: ImageControlHolder, image: Bitmap) {
            icHolder.imageImageView?.post { icHolder.imageImageView?.setImageBitmap(image) }
            expandImagePanel(icHolder)
        }

        // Aquí definimos dos constantes para identificar los dos diseños diferentes
        const val SELECTED_VIEW_TYPE = 1
        const val UNSELECTED_VIEW_TYPE = 2

        // region COLORS
        private var lotActiveForeColor: Int = 0
        private var notLotActiveForeColor: Int = 0
        private var notLotInactiveForeColor: Int = 0
        private var lotInactiveForeColor: Int = 0
        private var defaultForeColor: Int = 0

        private var lotActiveSelectedForeColor: Int = 0
        private var notLotActiveSelectedForeColor: Int = 0
        private var notLotInactiveSelectedForeColor: Int = 0
        private var lotInactiveSelectedForeColor: Int = 0
        private var defaultSelectedForeColor: Int = 0

        private var darkslategray: Int = 0
        private var lightgray: Int = 0

        /**
         * Setup colors
         * Simplemente inicializamos algunas variables con los colores que vamos a usar para cada estado.
         */
        private fun setupColors() {
            // Color de los diferentes estados
            val lotActive = getColor(context.resources, R.color.status_lot_active, null)
            val notLotActive = getColor(context.resources, R.color.status_not_lot_active, null)
            val lotInactive = getColor(context.resources, R.color.status_lot_inactive, null)
            val notLotInactive = getColor(context.resources, R.color.status_not_lot_inactive, null)
            val default = getColor(context.resources, R.color.status_default, null)

            // Mejor contraste para los ítems seleccionados
            lotActiveSelectedForeColor = getBestContrastColor(manipulateColor(lotActive, 0.5f))
            notLotActiveSelectedForeColor = getBestContrastColor(manipulateColor(notLotActive, 0.5f))
            notLotInactiveSelectedForeColor = getBestContrastColor(manipulateColor(notLotInactive, 0.5f))
            lotInactiveSelectedForeColor = getBestContrastColor(manipulateColor(lotInactive, 0.5f))
            defaultSelectedForeColor = getBestContrastColor(manipulateColor(default, 0.5f))

            // Mejor contraste para los ítems no seleccionados
            lotActiveForeColor = getBestContrastColor(lotActive)
            notLotActiveForeColor = getBestContrastColor(notLotActive)
            notLotInactiveForeColor = getBestContrastColor(notLotInactive)
            lotInactiveForeColor = getBestContrastColor(lotInactive)
            defaultForeColor = getBestContrastColor(default)

            // CheckBox color
            darkslategray = getColor(context.resources, R.color.darkslategray, null)

            // Title color
            lightgray = getColor(context.resources, R.color.lightgray, null)
        }
        // endregion

        /**
         * Get images thumbs
         *
         * @param adapter Parent adapter
         * @param holder ImageControl image holder
         * @param item Item of which we are requesting the image
         */
        private fun getImagesThumbs(
            adapter: ItemRecyclerAdapter,
            holder: ImageControlHolder,
            item: Item
        ) {
            if (!adapter.showImages) return

            Handler(Looper.getMainLooper()).postDelayed({
                adapter.run {
                    ImageAdapter.getImages(programData = ProgramData(
                        programObjectId = Table.item.tableId.toLong(), objId1 = item.itemId.toString()
                    ), onProgress = {
                        when (it.status) {
                            GetImageStatus.STARTING -> {
                                waitingImagePanel(holder)
                            }

                            GetImageStatus.NO_IMAGES -> {
                                idWithImage.remove(item.itemId)
                                collapseImagePanel(holder)
                            }

                            GetImageStatus.IMAGE_BROKEN, GetImageStatus.NO_AVAILABLE, GetImageStatus.IMAGE_AVAILABLE -> {
                                if (!idWithImage.contains(item.itemId)) {
                                    idWithImage.add(item.itemId)
                                }
                                val image = it.image
                                if (image != null) showImagePanel(holder, image)
                                else collapseImagePanel(holder)
                            }
                        }
                    })
                }
            }, 0)
        }
    }

    init {
        // Configuramos variables de estilo que se van a reutilizar.
        setupColors()

        // Por el momento no queremos animaciones, ni transiciones ante cambios en el DataSet
        recyclerView.itemAnimator = null

        // Vamos a retener en el caché un [cacheFactor] por ciento de los ítems creados o un máximo de [maxCachedItems]
        val maxCachedItems = 50
        val cacheFactor = 0.10
        var cacheSize = (fullList.size * cacheFactor).toInt()
        if (cacheSize > maxCachedItems) cacheSize = maxCachedItems
        recyclerView.setItemViewCacheSize(cacheSize)
        recyclerView.recycledViewPool.setMaxRecycledViews(SELECTED_VIEW_TYPE, 0)
        recyclerView.recycledViewPool.setMaxRecycledViews(UNSELECTED_VIEW_TYPE, 0)

        // Ordenamiento natural de la lista completa para trabajar en adelante con una lista ordenada
        val tList = sortItems(fullList)
        fullList = tList

        // Suministramos la lista a publicar refrescando el filtro que recorre la lista completa y devuelve los resultados filtrados y ordenados
        refreshFilter(filterOptions)

        // Cambiamos la visibilidad del panel de imágenes.
        if (!settingViewModel.useImageControl) showImages = false
        showImages(showImages)
    }

    /**
     * Return a sorted list of visible state items
     *
     * @param list
     * @return Lista ordenada con los estados visibles
     */
    private fun sortedVisibleList(list: MutableList<Item>?): MutableList<Item> {
        val croppedList = (list ?: mutableListOf()).mapNotNull { if (it.itemStatus in visibleStatus) it else null }
        return sortItems(croppedList.toMutableList())
    }

    // Sobrecargamos estos métodos para suministrar siempre una lista ordenada y filtrada por estado de visibilidad
    override fun submitList(list: MutableList<Item>?) {
        super.submitList(sortedVisibleList(list))
    }

    override fun submitList(list: MutableList<Item>?, commitCallback: Runnable?) {
        super.submitList(sortedVisibleList(list), commitCallback)
    }
}


