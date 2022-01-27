package io.github.bioastroiner.biotec.common.item;

import gregtech.api.items.metaitem.MetaItem;

import java.util.List;

public class BTMetaItems {
    public static List<MetaItem<?>> ITEMS = MetaItem.getMetaItems();

    public static void init() {
        BTMetaItem item = new BTMetaItem();
        item.setRegistryName("biotec_metaitem");
    }
}
