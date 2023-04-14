import javax.naming.ldap.LdapName

/*
Populate the below 'groupsList' with the names of the groups which you 
want to synchronize to STA along with their users. For example, 
groupsList = ["Group1", "Group2"], implies that only Group1 and Group2 
will be synchronized to STA.

NOTE: Only those groups will be synchronized to STA that are mentioned in the below list.
Also, only the users that are members of these groups will be synchronized to STA.
*/ 

def groupsList = []
def groupDNs = basic.getAttributeValues(shadow, 'memberOf')
def groupCNs = groupDNs?.collect {
new LdapName(it).getRdn(new LdapName(it).size() - 1)?.getValue()?.toString()?.toLowerCase()
} ?: []

return groupCNs.any { it in groupsList.collect { it?.toLowerCase() }}