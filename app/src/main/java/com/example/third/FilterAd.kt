package com.example.third

import android.widget.Filter
import com.example.third.adapters.AdapterAd
import com.example.third.models.ModelAd
import java.util.Locale

class FilterAd (
    private val adapter: AdapterAd,
    private val filterlist: ArrayList<ModelAd>

): Filter()  {
    override fun performFiltering(constraint: CharSequence?): FilterResults {

        var constraint = constraint
        val results = FilterResults()

        if (!constraint.isNullOrEmpty()) {
            constraint = constraint.toString().uppercase(Locale.getDefault())

            val filteredModels = ArrayList<ModelAd>()
            for (i in filterlist.indices) {
                if (filterlist[i].brand.uppercase(Locale.getDefault()).contains(constraint) ||
                    filterlist[i].category.uppercase(Locale.getDefault()).contains(constraint) ||
                    filterlist[i].condition.uppercase(Locale.getDefault()).contains(constraint) ||
                    filterlist[i].title.uppercase(Locale.getDefault()).contains(constraint)
                    ) {
                    filteredModels.add(filterlist[i])

                }
            }
            results.count = filteredModels.size
            results.values = filteredModels
        }else{
            results.count = filterlist.size
            results.values = filterlist

        }
        return results
    }



    override fun publishResults(constraint: CharSequence?, results: FilterResults) {

        adapter.adArraylist = results.values as ArrayList<ModelAd>
        adapter.notifyDataSetChanged()

    }
}