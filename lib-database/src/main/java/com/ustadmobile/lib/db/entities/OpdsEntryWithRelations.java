package com.ustadmobile.lib.db.entities;

import com.ustadmobile.lib.database.annotation.UmEmbedded;
import com.ustadmobile.lib.database.annotation.UmRelation;

import java.util.List;

/**
 * Created by mike on 1/13/18.
 */

public class OpdsEntryWithRelations extends OpdsEntry{

    @UmRelation(parentColumn = "id", entityColumn = "entryId")
    private List<OpdsLink> links;

    @UmRelation(parentColumn = "entryId", entityColumn = "entryId")
    private List<ContainerFileEntry> containerFileEntries;

    public List<OpdsLink> getLinks() {
        return links;
    }

    public void setLinks(List<OpdsLink> links) {
        this.links = links;
    }

}
