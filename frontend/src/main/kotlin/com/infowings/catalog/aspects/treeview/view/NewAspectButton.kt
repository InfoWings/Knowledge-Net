package com.infowings.catalog.aspects.treeview.view

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import react.RBuilder

fun RBuilder.newAspectButton(onClick: () -> Unit) =
    Button {
        attrs {
            icon = "add"
            intent = Intent.PRIMARY
            className = "bp3-minimal aspect-tree-view--button-new"
            this.onClick = { onClick() }
        }
        +"New Aspect"
    }

