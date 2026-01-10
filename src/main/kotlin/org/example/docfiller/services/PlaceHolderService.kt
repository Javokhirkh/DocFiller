package org.example.docfiller.services

import org.example.docfiller.Attach
import org.example.docfiller.PlaceHolder

interface PlaceHolderService {
    fun getPlaceHolderList(id:Long): List<PlaceHolder>
    fun extractPlaceHolders(attach: Attach): List<PlaceHolder>
    fun createAllPlaceHolders(placeHolders: List<PlaceHolder>)

}