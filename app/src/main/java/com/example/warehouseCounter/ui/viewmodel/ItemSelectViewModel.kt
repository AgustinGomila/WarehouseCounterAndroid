package com.example.warehouseCounter.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.example.warehouseCounter.data.room.entity.item.Item
import com.example.warehouseCounter.data.room.entity.itemCategory.ItemCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ItemSelectUiState(
    val title: String = "",
    val showSelectButton: Boolean = true,
    val multiSelect: Boolean = false,
    val hideFilterPanel: Boolean = false,
    val completeList: List<Item> = emptyList(),
    val checkedIds: Set<Long> = emptySet(),
    val currentScrollPosition: Int = 0,
    val filterItemDescription: String = "",
    val filterItemEan: String = "",
    val filterItemCategory: ItemCategory? = null,
    val filterItemExternalId: String = "",
    val filterOnlyActive: Boolean = true,
    val searchedText: String = "",
    val printQty: Int = 1,
    val templateId: Long = 0L,
    val showImages: Boolean = false,
    val showCheckBoxes: Boolean = false,
    val isLoading: Boolean = false,
    val lastSelected: Item? = null,
    val firstVisiblePos: Int = 1,
)

class ItemSelectViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemSelectUiState())
    val uiState: StateFlow<ItemSelectUiState> = _uiState.asStateFlow()

    init {
        // Inicializar estado con valores persistentes
        _uiState.value = ItemSelectUiState(
            multiSelect = savedStateHandle[ARG_MULTI_SELECT] ?: false,
            hideFilterPanel = savedStateHandle[ARG_HIDE_FILTER_PANEL] ?: false,
            completeList = savedStateHandle[ARG_COMPLETE_LIST] ?: emptyList(),
            checkedIds = savedStateHandle[ARG_CHECKED_IDS] ?: emptySet(),
            lastSelected = savedStateHandle[ARG_LAST_SELECTED],
            firstVisiblePos = savedStateHandle[ARG_FIRST_VISIBLE_POS] ?: 1,
            currentScrollPosition = savedStateHandle[ARG_CURRENT_SCROLL_POS] ?: 0,
            filterItemDescription = savedStateHandle[ARG_FILTER_DESC] ?: "",
            filterItemEan = savedStateHandle[ARG_FILTER_EAN] ?: "",
            filterItemCategory = savedStateHandle[ARG_FILTER_CATEGORY],
            filterItemExternalId = savedStateHandle[ARG_FILTER_EXTERNAL_ID] ?: "",
            filterOnlyActive = savedStateHandle[ARG_FILTER_ACTIVE] ?: true,
            searchedText = savedStateHandle[ARG_SEARCHED_TEXT] ?: "",
            printQty = savedStateHandle[ARG_PRINT_QTY] ?: 1,
            templateId = savedStateHandle[ARG_TEMPLATE_ID] ?: 0L,
            showImages = savedStateHandle[ARG_SHOW_IMAGES] ?: false,
            showCheckBoxes = savedStateHandle[ARG_SHOW_CHECKBOXES] ?: false,
            showSelectButton = savedStateHandle[ARG_SHOW_SELECT_BUTTON] ?: true,
        )
    }

    fun updateState(update: (ItemSelectUiState) -> ItemSelectUiState) {
        val newState = update(_uiState.value)

        // Persistir propiedades críticas
        savedStateHandle[ARG_TITLE] = newState.title
        savedStateHandle[ARG_MULTI_SELECT] = newState.multiSelect
        savedStateHandle[ARG_HIDE_FILTER_PANEL] = newState.hideFilterPanel
        savedStateHandle[ARG_SHOW_CHECKBOXES] = newState.showCheckBoxes
        savedStateHandle[ARG_SHOW_IMAGES] = newState.showImages
        savedStateHandle[ARG_SHOW_SELECT_BUTTON] = newState.showSelectButton

        _uiState.value = newState
    }

    val templateId get() = uiState.value.templateId
    val printQty: Int get() = uiState.value.printQty
    val searchedText: String get() = _uiState.value.searchedText
    val filterOnlyActive: Boolean get() = uiState.value.filterOnlyActive
    val filterItemExternalId: String get() = _uiState.value.filterItemExternalId
    val filterItemCategory: ItemCategory? get() = _uiState.value.filterItemCategory
    val filterItemEan: String get() = _uiState.value.filterItemEan
    val filterItemDescription: String get() = _uiState.value.filterItemDescription
    val hideFilterPanel: Boolean get() = uiState.value.hideFilterPanel
    val multiSelect: Boolean get() = uiState.value.multiSelect
    val completeList: List<Item>? get() = uiState.value.completeList
    val checkedIds: Set<Long> get() = uiState.value.checkedIds
    val lastSelected: Item? get() = uiState.value.lastSelected
    val currentScrollPosition: Int get() = uiState.value.currentScrollPosition

    fun applyFilters(
        description: String = "",
        ean: String = "",
        category: ItemCategory? = null,
        externalId: String = "",
        onlyActive: Boolean = true
    ) {
        updateState { state ->
            state.copy(
                filterItemDescription = description,
                filterItemEan = ean,
                filterItemCategory = category,
                filterItemExternalId = externalId,
                filterOnlyActive = onlyActive,
            )
        }
        loadItems()
    }

    fun loadItems() {
        val itemEan = uiState.value.filterItemEan.trim()
        val itemDescription = uiState.value.filterItemDescription.trim()
        val externalId = uiState.value.filterItemExternalId.trim()
        val itemCategory = uiState.value.filterItemCategory

        if (itemEan.isEmpty() && externalId.isEmpty() && itemDescription.isEmpty() && itemCategory == null) {
            _uiState.value = _uiState.value.copy(completeList = arrayListOf())
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            try {
                ItemCoroutines.getByQuery(
                    ean = itemEan,
                    externalId = externalId,
                    description = itemDescription,
                    itemCategoryId = itemCategory?.itemCategoryId,
                    useLike = true
                ) { items ->
                    updateState { it.copy(completeList = items, isLoading = false) }
                }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false) }
                Log.e(javaClass.simpleName, e.message.toString())
            }
        }
    }

    // Constantes para guardar estado
    companion object {
        const val ARG_TITLE = "title"
        const val ARG_SHOW_SELECT_BUTTON = "show_select_button"
        const val ARG_MULTI_SELECT = "multi_select"
        private const val ARG_HIDE_FILTER_PANEL = "hide_filter_panel"
        private const val ARG_LAST_SELECTED = "last_selected"
        private const val ARG_FIRST_VISIBLE_POS = "first_visible_pos"
        private const val ARG_COMPLETE_LIST = "complete_list"
        private const val ARG_CHECKED_IDS = "checked_ids"
        private const val ARG_CURRENT_SCROLL_POS = "current_scroll_pos"
        private const val ARG_FILTER_DESC = "filter_desc"
        private const val ARG_FILTER_EAN = "filter_ean"
        private const val ARG_FILTER_CATEGORY = "filter_category"
        private const val ARG_FILTER_ACTIVE = "filter_active"
        private const val ARG_FILTER_EXTERNAL_ID = "filter_external_id"
        private const val ARG_SEARCHED_TEXT = "searched_text"
        private const val ARG_PRINT_QTY = "print_qty"
        private const val ARG_TEMPLATE_ID = "template_id"
        private const val ARG_SHOW_IMAGES = "show_images"
        private const val ARG_SHOW_CHECKBOXES = "show_checkboxes"
    }
}