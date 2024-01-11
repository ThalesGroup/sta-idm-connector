/*
Populate the below 'groupsList' with the names of the groups which you 
want to synchronize to STA along with their users. For example, 
groupsList = ["Group1", "Group2"], implies that only Group1 and Group2 
will be synchronized to STA.

NOTE: Only those groups will be synchronized to STA that are mentioned in the below list.
Also, only the users that are members of these groups will be synchronized to STA.
*/ 

def groupsList = []
def groupNames = shadow.getAssociation().collect {
it.getShadowRef().getTargetName().toString()?.toLowerCase()
}

return groupNames.any { it in groupsList.collect { it?.toLowerCase()}}