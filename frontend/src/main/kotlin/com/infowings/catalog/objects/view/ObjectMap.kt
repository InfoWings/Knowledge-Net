package com.infowings.catalog.objects.view

import com.infowings.catalog.objects.ObjectLazyViewModel

fun objectMap(objects: List<ObjectLazyViewModel>) = objects.mapIndexed { index, value ->
    value.id to index
}.toMap()
