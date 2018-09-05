package com.mostc.pftt.model.custom;

public class NTFSAccessControlEntry {
	public ENTFSACEType perm;
	public ENTFSACEAction action;
	public ENTFSACETarget target;
	public String user = NTFSACL.EVERYONE;
	
	public NTFSAccessControlEntry(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target) {
		this.perm = perm;
		this.action = action;
		this.target = target;
	}
	
	public NTFSAccessControlEntry(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target, String user) {
		this(perm, action, target);
		this.user = user;
	}
	
	@Override
	public String toString() {
		return ( this.target+ ":" +this.action + " " + this.perm );
	}
	
	public boolean equals(NTFSAccessControlEntry entry) {
		return this.perm == entry.perm && this.action == entry.action && this.target == entry.target && (this.user==null||entry.user==null||this.user.equals(entry.user));
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this)
			return true;
		else if (o instanceof NTFSAccessControlEntry)
			return equals((NTFSAccessControlEntry)o);
		else
			return false;
	}
	
	@Override
	public int hashCode() {
		return (perm==null?1:perm.hashCode()) | (action==null?1:action.hashCode()) | (target==null?1:target.hashCode()) | (user==null?1:user.toLowerCase().hashCode());
	}
	
} // end public class NTFSAccessControlEntry
