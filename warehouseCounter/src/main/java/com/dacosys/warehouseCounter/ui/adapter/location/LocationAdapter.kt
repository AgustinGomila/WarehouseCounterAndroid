package com.dacosys.warehouseCounter.ui.adapter.location

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.dacosys.imageControl.adapter.ImageAdapter
import com.dacosys.imageControl.adapter.ImageAdapter.Companion.GetImageStatus
import com.dacosys.imageControl.adapter.ImageAdapter.Companion.ImageControlHolder
import com.dacosys.imageControl.network.common.ProgramData
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.LocationRowBinding
import com.dacosys.warehouseCounter.databinding.LocationRowExpandedBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Location
import com.dacosys.warehouseCounter.ktor.v2.dto.location.LocationType
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Status
import com.dacosys.warehouseCounter.misc.objects.table.Table
import com.dacosys.warehouseCounter.ui.adapter.FilterOptions
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getColorWithAlpha
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.manipulateColor
import java.util.*

class LocationAdapter private constructor(builder: Builder) :
    ListAdapter<Location, ViewHolder>(ItemDiffUtilCallback), Filterable {

    private val recyclerView: RecyclerView
    var fullList: ArrayList<Location> = ArrayList()
    var checkedHashArray: ArrayList<Int> = ArrayList()
    private var multiSelect: Boolean = false
    var showCheckBoxes: Boolean = false
    private var showCheckBoxesChanged: (Boolean) -> Unit = { }
    private var showImages: Boolean = false
    private var showImagesChanged: (Boolean) -> Unit = { }
    private var visibleStatus: ArrayList<Status> = ArrayList()
    private var filterOptions: FilterOptions = FilterOptions()

    // Este Listener debe usarse para los cambios de cantidad o de ítems marcados de la lista,
    // ya que se utiliza para actualizar los valores sumarios en la actividad.
    private var dataSetChangedListener: DataSetChangedListener? = null

    private var selectedItemChangedListener: SelectedItemChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null
    private var editItemRequiredListener: EditItemRequiredListener? = null

    // Listeners para los eventos de ImageControl.
    private var addPhotoRequiredListener: AddPhotoRequiredListener? = null
    private var albumViewRequiredListener: AlbumViewRequiredListener? = null

    // Posición del ítem seleccionado
    private var currentIndex = NO_POSITION

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

    private fun tableId(locationType: LocationType) = when (locationType) {
        LocationType.RACK -> Table.rack.tableId
        LocationType.WAREHOUSE -> Table.warehouse.tableId
        LocationType.WAREHOUSE_AREA -> Table.warehouseArea.tableId
    }

    /**
     * Change image control panel visibility on the bottom of the layout.
     * The state is defined by settingViewModel.useImageControl preference property.
     *
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun showImageControlPanel() {
        notifyItemRangeChanged(currentIndex, 1, PAYLOADS.IMAGE_CONTROL_VISIBILITY)
    }

    // Ids de ítems que tienen imágenes
    private var idWithImage: ArrayList<Int> = ArrayList()

    // Visibilidad del panel de miniaturas depende de la existencia de una imagen para ese ítem.
    private fun imageVisibility(hash: Int): Int {
        return if (showImages && idWithImage.contains(hash)) VISIBLE else GONE
    }

    // Permiso de edición de ítems
    private val userHasPermissionToEdit: Boolean by lazy { settingViewModel.editItems }

    fun clear() {
        checkedHashArray.clear()
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
        fun onSelectedItemChanged(item: Location?)
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
                SelectedViewHolder(
                    LocationRowExpandedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                UnselectedViewHolder(LocationRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
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
                        (holder as SelectedViewHolder).bindCheckBoxState(checkedHashArray.contains(item.hashCode))
                    else
                        (holder as UnselectedViewHolder).bindCheckBoxState(checkedHashArray.contains(item.hashCode))
                }

                PAYLOADS.IMAGE_VISIBILITY -> {
                    val item = getItem(position)
                    if (position == currentIndex) {
                        getImagesThumbs(this, (holder as SelectedViewHolder).icHolder, item)
                        holder.bindImageVisibility(
                            imageVisibility = imageVisibility(item.hashCode),
                            changingState = true
                        )
                    } else {
                        getImagesThumbs(this, (holder as UnselectedViewHolder).icHolder, item)
                        holder.bindImageVisibility(
                            imageVisibility = imageVisibility(item.hashCode),
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
            imageVisibility = imageVisibility(item.hashCode)
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
            imageVisibility = imageVisibility(item.hashCode)
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
    private fun setAlbumViewLogic(albumImageView: AppCompatImageView, item: Location) {
        albumImageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                albumViewRequiredListener?.onAlbumViewRequired(
                    tableId = tableId(item.locationType),
                    itemId = item.locationId
                )
            }
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setAddPhotoLogic(addPhotoImageView: AppCompatImageView, item: Location) {
        addPhotoImageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                addPhotoRequiredListener?.onAddPhotoRequired(
                    tableId = tableId(item.locationType),
                    itemId = item.locationId,
                    description = item.locationDescription
                )
            }
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setEditLogic(editImageView: AppCompatImageView, item: Location) {
        if (!userHasPermissionToEdit) {
            editImageView.visibility = GONE
            return
        }

        editImageView.visibility = VISIBLE

        editImageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                editItemRequiredListener?.onEditItemRequired(
                    tableId = tableId(item.locationType),
                    itemId = item.locationId
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
    private fun setCheckBoxLogic(checkBox: CheckBox, item: Location, position: Int) {
        val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            setChecked(item = item, isChecked = isChecked, suspendRefresh = true)
        }

        val longClickListener = OnLongClickListener { _ ->
            checkBox.setOnCheckedChangeListener(null)

            // Notificamos los cambios solo a los ítems que cambian de estado.
            val newState = !checkBox.isChecked
            if (newState) {
                currentList.mapIndexed { pos, item ->
                    if (item.hashCode !in checkedHashArray) {
                        checkedHashArray.add(item.hashCode)
                        notifyItemChanged(pos, PAYLOADS.CHECKBOX_STATE)
                    }
                }
            } else {
                currentList.mapIndexed { pos, item ->
                    if (item.hashCode in checkedHashArray) {
                        checkedHashArray.remove(item.hashCode)
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

        checkBox.isChecked = checkedHashArray.contains(item.hashCode)
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

    private fun mustBeVisible(it: Location): Boolean {
        return it.locationType == LocationType.RACK || it.locationStatus in visibleStatus
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            var selected: Location? = null
            var firstVisible: Location? = null

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                // Guardamos el item seleccionado y la posición del scroll
                selected = currentItem()
                var scrollPos =
                    (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition()
                        ?: NO_POSITION

                if (scrollPos != NO_POSITION && itemCount > scrollPos) {
                    var currentScrolled = getItem(scrollPos)

                    // Comprobamos si es visible el ítem del Scroll
                    if (mustBeVisible(currentScrolled))
                        firstVisible = currentScrolled
                    else {
                        // Si no es visible, intentar encontrar el próximo visible.
                        while (firstVisible == null) {
                            scrollPos++
                            if (itemCount > scrollPos) {
                                currentScrolled = getItem(scrollPos)
                                if (mustBeVisible(currentScrolled))
                                    firstVisible = currentScrolled
                            } else break
                        }
                    }
                }

                // Filtramos los resultados
                val results = FilterResults()
                var r: ArrayList<Location> = ArrayList()
                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    if (filterString.isNotEmpty()) {
                        var filterableItem: Location

                        for (i in 0 until fullList.size) {
                            filterableItem = fullList[i]

                            // Descartamos aquellos que no debe ser visibles
                            if (!mustBeVisible(filterableItem)) continue

                            if (isFilterable(filterableItem, filterString)) {
                                r.add(filterableItem)
                            }
                        }
                    } else if (filterOptions.showAllOnFilterEmpty) {
                        r = ArrayList(fullList.mapNotNull {
                            if (mustBeVisible(it)) it else null
                        })
                    }
                }

                results.values = r
                results.count = r.size
                return results
            }

            fun isFilterable(filterableItem: Location, filterString: String): Boolean =
                (filterableItem.locationDescription.contains(filterString, true) ||
                        filterableItem.locationParentStr.contains(filterString, true) ||
                        filterableItem.locationExternalId.contains(filterString, true))

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                submitList(results?.values as ArrayList<Location>) {
                    // Notificamos al Listener superior
                    dataSetChangedListener?.onDataSetChanged()

                    // Recuperamos el item seleccionado y la posición del scroll
                    if (firstVisible != null)
                        scrollToPos(getIndexByHashCode(firstVisible?.hashCode ?: -1), true)
                    if (selected != null)
                        selectItem(selected, false)
                }
            }
        }
    }

    private fun sortItems(originalList: MutableList<Location>): ArrayList<Location> {
        // Run the follow method on each of the roots
        return ArrayList(
            originalList.sortedWith(
                compareBy({ it.locationType },
                    { it.locationParentStr },
                    { it.locationDescription })
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

    fun add(item: Location, position: Int) {
        fullList.add(position, item)
        submitList(fullList) {
            // Notificamos al Listener superior
            dataSetChangedListener?.onDataSetChanged()
            selectItem(position)
        }
    }

    fun remove(position: Int) {
        val location = getItem(position)
        checkedHashArray.remove(location.hashCode)

        fullList.removeAt(position)
        submitList(fullList) {
            /** Notificamos al Listener superior */
            dataSetChangedListener?.onDataSetChanged()
        }
    }

    /**
     * Se utiliza cuando se edita un ítem y necesita actualizarse
     */
    fun updateItem(item: Location, scrollToPos: Boolean = false) {
        val t = fullList.firstOrNull { it == item } ?: return

        //// TODO: Update ítem

        submitList(fullList) {
            // Notificamos al Listener superior
            dataSetChangedListener?.onDataSetChanged()

            // Seleccionamos el ítem y hacemos scroll hasta él.
            selectItem(item, scrollToPos)
        }
    }

    fun selectItem(a: Location?, scroll: Boolean = true) {
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
        var item: Location? = null
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

    fun getIndexByHashCode(hashCode: Int): Int {
        return currentList.indexOfFirst { it.hashCode == hashCode }
    }

    private fun getIndex(item: Location): Int {
        return currentList.indexOfFirst { it == item }
    }

    private fun getItemByHashCode(hashCode: Int): Location? {
        return fullList.firstOrNull { it.hashCode == hashCode }
    }

    fun getAllChecked(): ArrayList<Location> {
        val items = ArrayList<Location>()
        checkedHashArray.mapNotNullTo(items) { getItemByHashCode(it) }
        return items
    }

    fun currentItem(): Location? {
        if (currentIndex == NO_POSITION) return null
        return if (currentList.any() && itemCount > currentIndex) getItem(currentIndex)
        else null
    }

    fun countChecked(): Int {
        return checkedHashArray.size
    }

    fun totalVisible(): Int {
        return itemCount
    }

    fun firstVisiblePos(): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        return layoutManager.findFirstVisibleItemPosition()
    }

    fun setChecked(item: Location, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val pos = getIndexByHashCode(item.hashCode)
        if (isChecked) {
            if (!checkedHashArray.contains(item.hashCode)) {
                checkedHashArray.add(item.hashCode)
            }
        } else {
            checkedHashArray.remove(item.hashCode)
        }

        checkedChangedListener?.onCheckedChanged(isChecked, pos)

        // Notificamos al Listener superior
        if (!suspendRefresh) dataSetChangedListener?.onDataSetChanged()
    }

    private var isFilling = false
    fun setChecked(items: ArrayList<Location>, isChecked: Boolean) {
        if (isFilling) return

        isFilling = true
        for (i in items) {
            setChecked(item = i, isChecked = isChecked, suspendRefresh = true)
        }
        isFilling = false

        // Notificamos al Listener superior
        dataSetChangedListener?.onDataSetChanged()
    }

    fun addVisibleStatus(status: Status) {
        if (visibleStatus.contains(status)) return
        visibleStatus.add(status)

        refreshFilter()
    }

    fun removeVisibleStatus(status: Status) {
        if (!visibleStatus.contains(status)) return
        visibleStatus.remove(status)

        // Quitamos los ítems con el estado seleccionado de la lista marcados.
        val uncheckedItems = ArrayList(fullList.mapNotNull { if (it.locationStatus == status) it.hashCode else null })
        checkedHashArray.removeAll(uncheckedItems.toSet())

        refreshFilter()
    }

    internal class SelectedViewHolder(val binding: LocationRowExpandedBinding) :
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

        fun bind(item: Location, checkBoxVisibility: Int = GONE, imageVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)
            bindImageControlVisibility(visibility = if (settingViewModel.useImageControl) VISIBLE else GONE)
            bindImageVisibility(imageVisibility = imageVisibility, changingState = false)

            binding.locationTextView.text = item.locationDescription

            val parentStr = item.locationParentStr
            if (parentStr.isNotEmpty()) {
                binding.parentTextView.text = parentStr
                binding.parentTextView.visibility = VISIBLE
                binding.dividerInternal2.visibility = VISIBLE
            } else {
                binding.parentTextView.text = ""
                binding.parentTextView.visibility = GONE
                binding.dividerInternal2.visibility = GONE
            }

            val extId = item.locationExternalId
            if (extId.isNotEmpty()) {
                binding.extIdCheckedTextView.text = item.locationExternalId
                binding.extIdPanel.visibility = VISIBLE
                binding.dividerInternal3.visibility = VISIBLE
            } else {
                binding.extIdCheckedTextView.text = ""
                binding.extIdPanel.visibility = GONE
                binding.dividerInternal3.visibility = GONE
            }

            setStyle(item)
        }

        private fun setStyle(item: Location) {
            val v = itemView

            // Background layouts
            // Resalta por estado del ítem
            val layoutWActive = getDrawable(context.resources, R.drawable.layout_thin_border_w, null)
            val layoutWInactive = getDrawable(context.resources, R.drawable.layout_thin_border_w_inactive, null)
            val layoutWaActive = getDrawable(context.resources, R.drawable.layout_thin_border_wa, null)
            val layoutWaInactive = getDrawable(context.resources, R.drawable.layout_thin_border_wa_inactive, null)
            val layoutRActive = getDrawable(context.resources, R.drawable.layout_thin_border_r, null)
            val layoutRInactive = getDrawable(context.resources, R.drawable.layout_thin_border_r_inactive, null)

            val backColor: Drawable
            val foreColor: Int

            when (item.locationType) {
                LocationType.RACK -> {
                    when (item.locationActive) {
                        true -> {
                            backColor = layoutRActive!!
                            foreColor = activeRSelectedForeColor
                        }

                        false -> {
                            backColor = layoutRInactive!!
                            foreColor = inactiveRSelectedForeColor
                        }
                    }
                }

                LocationType.WAREHOUSE_AREA -> {
                    when (item.locationActive) {
                        true -> {
                            backColor = layoutWaActive!!
                            foreColor = activeWaSelectedForeColor
                        }

                        false -> {
                            backColor = layoutWaInactive!!
                            foreColor = inactiveWaSelectedForeColor
                        }
                    }
                }

                LocationType.WAREHOUSE -> {
                    when (item.locationActive) {
                        true -> {
                            backColor = layoutWActive!!
                            foreColor = activeWSelectedForeColor
                        }

                        false -> {
                            backColor = layoutWInactive!!
                            foreColor = inactiveWSelectedForeColor
                        }
                    }
                }
            }

            val titleForeColor: Int = manipulateColor(foreColor, 0.8f)

            v.background = backColor
            binding.locationTextView.setTextColor(foreColor)
            binding.parentTextView.setTextColor(foreColor)
            binding.extIdCheckedTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)

            // Titles
            binding.locationTextView.setTextColor(titleForeColor)
            binding.parentTextView.setTextColor(titleForeColor)
            binding.extIdLabelTextView.setTextColor(titleForeColor)

            // ImageControl
            binding.signImageView.imageTintList = ColorStateList.valueOf(titleForeColor)
            binding.addPhotoImageView.imageTintList = ColorStateList.valueOf(titleForeColor)
            binding.albumImageView.imageTintList = ColorStateList.valueOf(titleForeColor)
        }
    }

    internal class UnselectedViewHolder(val binding: LocationRowBinding) :
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

        fun bind(item: Location, checkBoxVisibility: Int = GONE, imageVisibility: Int = GONE) {
            bindCheckBoxVisibility(checkBoxVisibility)
            bindImageVisibility(imageVisibility = imageVisibility, changingState = false)

            binding.locationTextView.text = item.locationDescription

            val parentStr = item.locationParentStr
            if (parentStr.isNotEmpty()) {
                binding.parentTextView.text = parentStr
                binding.parentTextView.visibility = VISIBLE
                binding.dividerInternal.visibility = VISIBLE
            } else {
                binding.parentTextView.text = ""
                binding.parentTextView.visibility = GONE
                binding.dividerInternal.visibility = GONE
            }

            setStyle(item)
        }

        private fun setStyle(item: Location) {
            val v = itemView

            // Background layouts
            // Resalta por estado del ítem
            val layoutWActive = getDrawable(context.resources, R.drawable.layout_thin_border_w, null)
            val layoutWInactive = getDrawable(context.resources, R.drawable.layout_thin_border_w_inactive, null)
            val layoutWaActive = getDrawable(context.resources, R.drawable.layout_thin_border_wa, null)
            val layoutWaInactive = getDrawable(context.resources, R.drawable.layout_thin_border_wa_inactive, null)
            val layoutRActive = getDrawable(context.resources, R.drawable.layout_thin_border_r, null)
            val layoutRInactive = getDrawable(context.resources, R.drawable.layout_thin_border_r_inactive, null)

            val backColor: Drawable
            val foreColor: Int
            val titleForeColor: Int = darkslategray

            when (item.locationType) {
                LocationType.RACK -> {
                    when (item.locationActive) {
                        true -> {
                            backColor = layoutRActive!!
                            foreColor = activeRSelectedForeColor
                        }

                        false -> {
                            backColor = layoutRInactive!!
                            foreColor = inactiveRSelectedForeColor
                        }
                    }
                }

                LocationType.WAREHOUSE_AREA -> {
                    when (item.locationActive) {
                        true -> {
                            backColor = layoutWaActive!!
                            foreColor = activeWaSelectedForeColor
                        }

                        false -> {
                            backColor = layoutWaInactive!!
                            foreColor = inactiveWaSelectedForeColor
                        }
                    }
                }

                LocationType.WAREHOUSE -> {
                    when (item.locationActive) {
                        true -> {
                            backColor = layoutWActive!!
                            foreColor = activeWSelectedForeColor
                        }

                        false -> {
                            backColor = layoutWInactive!!
                            foreColor = inactiveWSelectedForeColor
                        }
                    }
                }
            }

            v.background = backColor
            binding.parentTextView.setTextColor(foreColor)
            binding.locationTextView.setTextColor(foreColor)
            binding.checkBox.buttonTintList = ColorStateList.valueOf(titleForeColor)
        }
    }

    private object ItemDiffUtilCallback : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem.hashCode == newItem.hashCode
        }

        override fun areContentsTheSame(oldItem: Location, newItem: Location): Boolean {
            if (oldItem.locationId != newItem.locationId) return false
            if (oldItem.locationType != newItem.locationType) return false
            return oldItem.hashCode == newItem.hashCode
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
        private var activeWForeColor: Int = 0
        private var activeWaForeColor: Int = 0
        private var activeRForeColor: Int = 0
        private var inactiveWForeColor: Int = 0
        private var inactiveWaForeColor: Int = 0
        private var inactiveRForeColor: Int = 0
        private var defaultForeColor: Int = 0

        private var activeWSelectedForeColor: Int = 0
        private var activeWaSelectedForeColor: Int = 0
        private var activeRSelectedForeColor: Int = 0
        private var inactiveWSelectedForeColor: Int = 0
        private var inactiveWaSelectedForeColor: Int = 0
        private var inactiveRSelectedForeColor: Int = 0
        private var defaultSelectedForeColor: Int = 0

        private var darkslategray: Int = 0
        private var lightgray: Int = 0

        /**
         * Setup colors
         * Simplemente inicializamos algunas variables con los colores que vamos a usar para cada estado.
         */
        private fun setupColors() {
            // Color de los diferentes estados
            val activeW = getColor(context.resources, R.color.status_w_active, null)
            val activeWa = getColor(context.resources, R.color.status_wa_active, null)
            val activeR = getColor(context.resources, R.color.status_r_active, null)
            val inactiveW = getColor(context.resources, R.color.status_w_inactive, null)
            val inactiveWa = getColor(context.resources, R.color.status_wa_inactive, null)
            val inactiveR = getColor(context.resources, R.color.status_r_inactive, null)
            val default = getColor(context.resources, R.color.status_default, null)

            // Mejor contraste para los ítems seleccionados
            activeWSelectedForeColor = getBestContrastColor(manipulateColor(activeW, 0.5f))
            activeWaSelectedForeColor = getBestContrastColor(manipulateColor(activeWa, 0.5f))
            activeRSelectedForeColor = getBestContrastColor(manipulateColor(activeR, 0.5f))
            inactiveWSelectedForeColor = getBestContrastColor(manipulateColor(inactiveW, 0.5f))
            inactiveWaSelectedForeColor = getBestContrastColor(manipulateColor(inactiveWa, 0.5f))
            inactiveRSelectedForeColor = getBestContrastColor(manipulateColor(inactiveR, 0.5f))
            defaultSelectedForeColor = getBestContrastColor(manipulateColor(default, 0.5f))

            // Mejor contraste para los ítems no seleccionados
            activeWForeColor = getBestContrastColor(activeW)
            activeWaForeColor = getBestContrastColor(activeWa)
            activeRForeColor = getBestContrastColor(activeR)
            inactiveWForeColor = getBestContrastColor(inactiveW)
            inactiveWaForeColor = getBestContrastColor(inactiveWa)
            inactiveRForeColor = getBestContrastColor(inactiveR)
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
         * @param item Location of which we are requesting the image
         */
        private fun getImagesThumbs(
            adapter: LocationAdapter,
            holder: ImageControlHolder,
            item: Location
        ) {
            if (!adapter.showImages) return

            Handler(Looper.getMainLooper()).postDelayed({
                adapter.run {
                    ImageAdapter.getImages(
                        programData = ProgramData(
                            programObjectId = tableId(item.locationType).toLong(),
                            objId1 = item.locationId.toString()
                        ), onProgress = {
                            when (it.status) {
                                GetImageStatus.STARTING -> {
                                    waitingImagePanel(holder)
                                }

                                GetImageStatus.NO_IMAGES -> {
                                    idWithImage.remove(item.hashCode)
                                    collapseImagePanel(holder)
                                }

                                GetImageStatus.IMAGE_BROKEN, GetImageStatus.NO_AVAILABLE, GetImageStatus.IMAGE_AVAILABLE -> {
                                    if (!idWithImage.contains(item.hashCode)) {
                                        idWithImage.add(item.hashCode)
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
        // Set values from Builder
        recyclerView = builder.recyclerView
        fullList = builder.fullList
        checkedHashArray = builder.checkedHashArray
        multiSelect = builder.multiSelect
        showCheckBoxes = builder.showCheckBoxes
        showCheckBoxesChanged = builder.showCheckBoxesChanged
        showImages = builder.showImages
        showImagesChanged = builder.showImagesChanged
        visibleStatus = builder.visibleStatus
        filterOptions = builder.filterOptions

        // TODO: Dummy status list, FIX Later
        if (visibleStatus.isEmpty()) {
            for (i in 0..19) {
                visibleStatus.add(Status(i.toLong(), """${context.getString(R.string.status)} $i"""))
            }
        }

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
    private fun sortedVisibleList(list: MutableList<Location>?): MutableList<Location> {
        val croppedList = (list ?: mutableListOf()).mapNotNull { if (mustBeVisible(it)) it else null }
        return sortItems(croppedList.toMutableList())
    }

    // Sobrecargamos estos métodos para suministrar siempre una lista ordenada y filtrada por estado de visibilidad
    override fun submitList(list: MutableList<Location>?) {
        super.submitList(sortedVisibleList(list))
    }

    override fun submitList(list: MutableList<Location>?, commitCallback: Runnable?) {
        super.submitList(sortedVisibleList(list), commitCallback)
    }

    class Builder {
        fun build(): LocationAdapter {
            return LocationAdapter(this)
        }

        internal lateinit var recyclerView: RecyclerView
        internal var fullList: ArrayList<Location> = ArrayList()
        internal var checkedHashArray: ArrayList<Int> = ArrayList()
        internal var multiSelect: Boolean = false
        internal var showCheckBoxes: Boolean = false
        internal var showCheckBoxesChanged: (Boolean) -> Unit = { }
        internal var showImages: Boolean = false
        internal var showImagesChanged: (Boolean) -> Unit = { }
        internal var visibleStatus: ArrayList<Status> = ArrayList()
        internal var filterOptions: FilterOptions = FilterOptions()

        internal var dataSetChangedListener: DataSetChangedListener? = null
        internal var selectedItemChangedListener: SelectedItemChangedListener? = null
        internal var checkedChangedListener: CheckedChangedListener? = null
        internal var editItemRequiredListener: EditItemRequiredListener? = null
        internal var addPhotoRequiredListener: AddPhotoRequiredListener? = null
        internal var albumViewRequiredListener: AlbumViewRequiredListener? = null

        // Setter methods for variables with chained methods
        @Suppress("unused")
        fun recyclerView(`val`: RecyclerView): Builder {
            recyclerView = `val`
            return this
        }

        @Suppress("unused")
        fun fullList(`val`: ArrayList<Location>): Builder {
            fullList = `val`
            return this
        }

        @Suppress("unused")
        fun checkedHashArray(`val`: ArrayList<Int>): Builder {
            checkedHashArray = `val`
            return this
        }

        @Suppress("unused")
        fun multiSelect(`val`: Boolean): Builder {
            multiSelect = `val`
            return this
        }

        @Suppress("unused")
        fun showCheckBoxes(`val`: Boolean, listener: (Boolean) -> Unit): Builder {
            showCheckBoxes = `val`
            showCheckBoxesChanged = listener
            return this
        }

        @Suppress("unused")
        fun showImages(`val`: Boolean, listener: (Boolean) -> Unit): Builder {
            showImages = `val`
            showImagesChanged = listener
            return this
        }

        @Suppress("unused")
        fun visibleStatus(`val`: ArrayList<Status>): Builder {
            visibleStatus = `val`
            return this
        }

        @Suppress("unused")
        fun filterOptions(`val`: FilterOptions): Builder {
            filterOptions = `val`
            return this
        }

        // Setter methods for variables with chained methods
        @Suppress("unused")
        fun dataSetChangedListener(listener: DataSetChangedListener?): Builder {
            dataSetChangedListener = listener
            return this
        }

        @Suppress("unused")
        fun selectedItemChangedListener(listener: SelectedItemChangedListener?): Builder {
            selectedItemChangedListener = listener
            return this
        }

        @Suppress("unused")
        fun checkedChangedListener(listener: CheckedChangedListener?): Builder {
            checkedChangedListener = listener
            return this
        }

        @Suppress("unused")
        fun editItemRequiredListener(listener: EditItemRequiredListener?): Builder {
            editItemRequiredListener = listener
            return this
        }

        @Suppress("unused")
        fun addPhotoRequiredListener(listener: AddPhotoRequiredListener?): Builder {
            addPhotoRequiredListener = listener
            return this
        }

        @Suppress("unused")
        fun albumViewRequiredListener(listener: AlbumViewRequiredListener?): Builder {
            albumViewRequiredListener = listener
            return this
        }
    }
}
