package com.mostc.pftt.model.custom;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.Host;

/** Represents an Access-Control-List (ACL), specifically an ACL for the NTFS File system.
 * 
 * Can generate all valid permutations of ACLs for functional testing on NTFS.
 * 
 * Based on ACL-Test (JScript) written by Ryan Biesemeyer.
 * 
 * NOTE: this class doesn't use PHP to set or check ACLs at all. Rather it sets up the ACLs
 * so that a test case can then use PHP to check them.
 * 
 * @author Matt Ficken
 * @author Ryan Biesemeyer
 *
 */

public class NTFSACL {
	public static final String EVERYONE = "Everyone";
	//
	protected NTFSACL parent;
	protected final LinkedList<NTFSAccessControlEntry> grants, denys;
	protected String path;
	
	public NTFSACL(NTFSACL parent) {
		grants = new LinkedList<NTFSAccessControlEntry>();
		denys = new LinkedList<NTFSAccessControlEntry>();
		this.parent = parent;
	}
	
	public NTFSACL(NTFSACL parent, String part) {
		this(parent);
		this.path = parent.path+"/"+part;
	}
	
	public NTFSACL(String path) {
		grants = new LinkedList<NTFSAccessControlEntry>();
		denys = new LinkedList<NTFSAccessControlEntry>();
		this.path = path;
	}
	
	public void grant(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target) {
		grant(new NTFSAccessControlEntry(perm, action, target));
	}
	
	public void grant(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target, String user) {
		grant(new NTFSAccessControlEntry(perm, action, target, user));
	}
	
	public void grant(NTFSAccessControlEntry permission) {
		this.grants.add(permission);
	}
	
	public void deny(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target) {
		deny(new NTFSAccessControlEntry(perm, action, target));
	}
	
	public void deny(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target, String user) {
		deny(new NTFSAccessControlEntry(perm, action, target, user));
	}
	
	public void deny(NTFSAccessControlEntry permission) {
		this.denys.add(permission);
	}
	
	public void revokeGrant(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target) {
		revokeGrant(new NTFSAccessControlEntry(perm, action, target));
	}
	
	public void revokeGrant(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target, String user) {
		revokeGrant(new NTFSAccessControlEntry(perm, action, target, user));
	}
	
	public void revokeGrant(NTFSAccessControlEntry permission) {
		this.grants.remove(permission);
	}
	
	public void revokeDeny(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target) {
		revokeDeny(new NTFSAccessControlEntry(perm, action, target));
	}
	
	public void revokeDeny(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target, String user) {
		revokeDeny(new NTFSAccessControlEntry(perm, action, target, user));
	}
	
	public void revokeDeny(NTFSAccessControlEntry permission) {
		this.denys.remove(permission);
	}
	
	public boolean isGranted(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target) {
		return isGranted(new NTFSAccessControlEntry(perm, action, target));
	}
	
	public boolean isGranted(ENTFSACEType perm) {
		for (NTFSAccessControlEntry ace:grants) {
			if (ace.perm==perm)
				return true;
		}
		return false;
	}
	
	public boolean isGranted(ENTFSACEType perm, String user) {
		for (NTFSAccessControlEntry ace:grants) {
			if (ace.perm==perm && ace.user != null && ace.user.equals(user));
				return true;
		}
		return false;
	}
	
	public boolean isGranted(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target, String user) {
		return isGranted(new NTFSAccessControlEntry(perm, action, target, user));
	}
	
	public boolean isGranted(NTFSAccessControlEntry ace) {
		return grants.contains(ace);
	}
	
	public boolean isDenied(ENTFSACEType perm) {
		for (NTFSAccessControlEntry ace:denys) {
			if (ace.perm==perm)
				return true;
		}
		return false;
	}
	
	public boolean isDenied(ENTFSACEType perm, String user) {
		for (NTFSAccessControlEntry ace:denys) {
			if (ace.perm==perm && ace.user != null && ace.user.equals(user));
				return true;
		}
		return false;
	}
	
	public boolean isDenied(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target) {
		return isDenied(new NTFSAccessControlEntry(perm, action, target));
	}
	
	public boolean isDenied(ENTFSACEType perm, ENTFSACEAction action, ENTFSACETarget target, String user) {
		return isDenied(new NTFSAccessControlEntry(perm, action, target, user));
	}
	
	public boolean isDenied(NTFSAccessControlEntry ace) {
		return denys.contains(ace);
	}
	
	/**
	 * Generate bottom-up, then call this.apply() on the top
	 * @throws Exception 
	 */
	public String generate(Host host) throws Exception {
		return generate(host, EVERYONE);
	}
	
	public String generate(Host host, String forUser) throws Exception {
		return generate(host, forUser, 0);
	}
	
	public String generate(Host host, String forUser, int depth) throws Exception {
		if (forUser==null)
			forUser = EVERYONE;
		if (depth<0)
			depth = 0;
		
		String parentPath;
		if( this.parent instanceof NTFSACL ){
		// If this has a parent ACL, generate the folder for it first
			parentPath = this.parent.generate(host, forUser, ( depth + 1 ));
		} else { 
		// parent is the rootpath. Generate a new folder in the rootPath that blocks.
			parentPath = host.joinIntoOnePath(
				this.parent.path,
				host.mCreateTempName(getClass(), "parentPath")
			);
			host.mCreateDirs(parentPath);
			this.path = parentPath;
			doGrant(host, parentPath, ENTFSACEType.FULL, null, true);
			doBlock(host, parentPath);
			this.parent = new NTFSACL(parentPath);
		}
		
		this.path = host.joinIntoOnePath(
				parentPath, 
				host.mCreateTempName(getClass(), "path")
			);
		
		// if we're at a depth of zero, target is a file. else, it's a folder,
		if( depth > 0 ) {
			host.mCreateDirs(this.path);
		} else {
			host.mSaveTextFile(this.path, Long.toString(System.currentTimeMillis()));
		}
		
		
		if( depth <= 0 ) {
			// now let's apply.
			this.apply(host, forUser);
		}
		return this.path;
	}
	
	/**
	 * Apply top-down. Strip all, then grant/deny
	 * @throws Exception 
	 */
	public void apply(Host host) throws Exception {
		apply(host, EVERYONE);
	}
	
	public void apply(Host host, String forUser) throws Exception {
		if (forUser==null)
			forUser = EVERYONE;
		
		NTFSACL.doClear(host, this.path);
		for( NTFSAccessControlEntry g : this.grants ){
			doGrant(host, this.path, g.perm);
		}
		for( NTFSAccessControlEntry d : this.denys ){
			doDeny(host, this.path, d.perm);
		}
		
		if( this.parent != null ) {
			this.parent.apply(host);
		} else {
			//doClear( this.block );
		}
	}
	
	public void destroy(Host host) throws Exception {
		destroy(host, 0);
	}
	
	public void destroy(Host host, int depth) throws Exception {
		if (depth<0)
			depth = 0;
								
		doClear(host, this.path);
		doGrant(host, this.path, ENTFSACEType.FULL, null, true);
			
		if( this.parent != null ) {
			this.parent.destroy(host);
		} else if ( this.path != null ) {
			doClear(host, this.path, true);
			doGrant(host, this.path, ENTFSACEType.FULL, null, true);
			host.mDeleteIfExists(this.path);
		} else {
			// nothing to see here.
		}
	}
	
	@Override
	public String toString() {
		return toString(false);
	}
	
	public String toString(boolean not_root) {
		StringBuilder ret = new StringBuilder();
		toString(ret, not_root);
		return ret.toString();
	}
	
	protected void toString(StringBuilder ret, boolean not_root) {
		if( !(not_root||false) ) {
			ret.append("APPLIED TO:    ");ret.append(this.path);ret.append("\n");
		}
		for( Object perm : this.grants ) {
			ret.append("  GRANT ");ret.append(perm);ret.append("\n");
		}
		for( Object perm : this.denys ) {
			ret.append("  DENY ");ret.append(perm);ret.append("\n");
		}
		if (this.grants.isEmpty() && this.denys.isEmpty()) {
			ret.append("  NONE\n");
		}
		if( this.parent != null ) {
			ret.append("INHERITS FROM: ");ret.append(this.parent.path);ret.append("\n");
			this.parent.toString(ret, true);ret.append("\n");
		} else if (StringUtil.isNotEmpty(this.parent.path)) {
			ret.append("INHERITS FROM: ");ret.append(this.parent);ret.append("\n");
			ret.append("  BLOCKING GRANT FULL\n");
		}
	} 
	
	public boolean equals(NTFSACL o) {
		if (path!=null && o.path!=null && !path.equals(o.path))
			return false;
		else if (!grants.equals(o.grants))
			return false;
		else if (!denys.equals(o.denys))
			return false;
		else if (parent != null && o.parent != null && !parent.equals(o.parent))
			return false;
		else
			return true;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this)
			return true;
		else if (o instanceof NTFSACL)
			return equals((NTFSACL)o);
		else
			return false;
	}
	
	@Override
	public int hashCode() {
		return (parent==null?1:parent.hashCode()) | (path==null?1:path.hashCode()) | grants.hashCode() | denys.hashCode();
	}
	
	public static enum EReductionMode {
		REPEAT_INHERITED,
		CANCEL_DIRECT,
		CANCEL_INHERITED
	}
	
	public void permute(Host host, String directory_root, IPermutationCallback callback_function) {
		permute(host, EReductionMode.REPEAT_INHERITED, directory_root, callback_function);
	}
	
	public void permute(Host host, String directory_root, IPermutationCallback callback_function, boolean do_setup) {
		permute(host, EReductionMode.REPEAT_INHERITED, directory_root, callback_function, do_setup);
	}
	
	public void permute(Host host, String directory_root, IPermutationCallback callback_function, Object callback_arguments) {
		permute(host, EReductionMode.REPEAT_INHERITED, directory_root, callback_function, callback_arguments);
	}
	
	public void permute(Host host, String directory_root, IPermutationCallback callback_function, Object callback_arguments, boolean do_setup) {
		permute(host, EReductionMode.REPEAT_INHERITED, directory_root, callback_function, callback_arguments, do_setup);
	}
	
	public void permute(Host host, EReductionMode reduction, String directory_root, IPermutationCallback callback_function) {
		permute(host, reduction, directory_root, callback_function, null);
	}
	
	public void permute(Host host, EReductionMode reduction, String directory_root, IPermutationCallback callback_function, boolean do_setup) {
		permute(host, reduction, directory_root, callback_function, null, do_setup);
	}
	
	public void permute(Host host, EReductionMode reduction, String directory_root, IPermutationCallback callback_function, Object callback_arguments) {
		permute(host, reduction, directory_root, callback_function, callback_arguments, false);
	}
	
	public void permute(Host host, EReductionMode reduction, String directory_root, IPermutationCallback callback_function, Object callback_arguments, boolean do_setup) {
		permute(host, reduction, directory_root, callback_function, callback_arguments, do_setup);
	}
	
	/** Run a callback function for every selected permutation.
	 * 
	 * @param host
	 * @param reduction - reducing out certain knon-cancelling permutations.
	 * @param directory_root - where you want the ACL structure to be built
	 * @param callback_function
	 * @param callback_arguments - passed unmodified to callback_function as optional_params
	 * @param do_setup - setup the ACL
	 * @param log_pw - log output
	 * @throws Exception 
	 */
	public void permute(Host host, EReductionMode reduction, String directory_root, IPermutationCallback callback_function, Object callback_arguments, boolean do_setup, PrintWriter log_pw) throws Exception {
		LinkedList<NTFSAccessControlEntry> availablePerms = new LinkedList<NTFSAccessControlEntry>();
		ENTFSACEType[] permList = ENTFSACEType.values();
		ENTFSACEAction[] actionList = new ENTFSACEAction[]{ENTFSACEAction.GRANT, ENTFSACEAction.DENY};
		ENTFSACETarget[] targetList = new ENTFSACETarget[]{ENTFSACETarget.INHERIT, ENTFSACETarget.DIRECT};
		
		// Build up our permList
		for( ENTFSACEType p : permList ) {
			for( ENTFSACEAction a : actionList ) {
				for( ENTFSACETarget t : targetList ) {
					availablePerms.add(new NTFSAccessControlEntry(p, a, t));
				}
			}
		}
		
		ArrayList<NTFSAccessControlEntry> appliedPerms = new ArrayList<NTFSAccessControlEntry>((int)Math.max(32768, Math.pow(availablePerms.size(), 2)));
		
		// supply recursive args to recursive _permute function
		_permute(host, reduction, directory_root, callback_function, callback_arguments, availablePerms, appliedPerms, do_setup, log_pw);
	}
	
	/**
	 * Internal code to actually do the permutations, along with reductions
	 * @param {String} directory_root - where you want the ACL structure to be built
	 * @param {Array|Object} return_object - should implement push(),length,and pop()
	 * @param {Function} callback_function( build_path, ace_directives, optional_params )
	 * @param {Object} callback_arguments - passed unmodified to callback_function as optional_params
	 * @param {Object} params; acceptable keys are:
	 *    reduction - bitwise value reducing out certain known-cancelling permutations.
	 * 	  no_set  - true if we do *not* set ACLs before calling callback_function
	 * @param {Array} available_perms - a list of perms that have not been applied
	 * @param {Array} applied_perms - a list of perms that have been applied
	 * @throws Exception 
	 */
	protected void _permute(Host host, EReductionMode reduction, String directory_root, IPermutationCallback callback_function, Object callback_arguments, LinkedList<NTFSAccessControlEntry> available_perms, ArrayList<NTFSAccessControlEntry> applied_perms, boolean do_setup, PrintWriter log_pw) throws Exception {
		// clone input param stacks
		if ( available_perms.size() > 0 ) { // we have more available perms. go deeper.
			NTFSAccessControlEntry top_perm = available_perms.removeFirst();

			//descend without
			this._permute(
				host, 
				reduction,
				directory_root,
				callback_function,
				callback_arguments,
				available_perms,
				applied_perms,
				do_setup,
				log_pw
			);
			
			// descend with
			applied_perms.add(top_perm);
			this._permute(
				host,
				reduction,
				directory_root,
				callback_function,
				callback_arguments,
				available_perms,
				applied_perms,
				do_setup,
				log_pw
			);
		} else {
			// no more available perms. sets are final.
			NTFSACL parentACL = new NTFSACL(directory_root);
			NTFSACL childACL = new NTFSACL(parentACL);
			
			for( NTFSAccessControlEntry k : applied_perms ){
				NTFSACL targetACL = ( k.target == ENTFSACETarget.DIRECT ? childACL : parentACL );
				switch(k.action){
				case GRANT:
					NTFSACL.doGrant(host, targetACL.path, k);
					break;
				case DENY:
					NTFSACL.doDeny(host, targetACL.path, k);
					break;
				default:
				}
			}
			
			// If specified, skip sets with inheritances that are repeated as directs.
			// e.g., if a GRANT READ is inherited, the inheritance is moot if the child also has a GRANT READ.
			switch(reduction) {
			case REPEAT_INHERITED:
				for ( NTFSAccessControlEntry p0 : applied_perms ) {
					if ( p0.target == ENTFSACETarget.INHERIT ) {
						for ( NTFSAccessControlEntry p1 : applied_perms ){
							if ( p1.target == ENTFSACETarget.DIRECT && p1.perm.equals(p0.perm) && p1.action == p0.action ) {
								
								if (log_pw!=null)
									log_pw.println("SKIP: INHERITED GRANT "+p0.perm+" IS IGNORED WHEN DIRECT GRANT "+p0.perm+" IS PRESENT");
								
								return;
							}
						}
					}
				} // end for
				break;
			// if we're granting something that also has a same-level deny, it has no effect.
			// e.g. a DIRECT GRANT READ is *always* overriden by DIRECT DENY READ
			case CANCEL_DIRECT:
				for ( NTFSAccessControlEntry p0 : applied_perms ){
					if ( p0.action == ENTFSACEAction.GRANT ) {
						for ( NTFSAccessControlEntry p2 : applied_perms ) {
							if ( p2.action == ENTFSACEAction.DENY && p2.perm.equals(p0.perm) && p2.target == p0.target ) {
								
								if (log_pw!=null)
									log_pw.println("SKIP: GRANT "+p0.perm+" NEVER OVERRIDES A DENY "+p0.perm+" ON THE SAME OBJECT");
								
								return;
							}
						}
					}
				} // end for
				break;
			// if we're granting something that also inherits a denial of the same kind, skip it
			// e.g. a INHERITED DENY READ is *always* overriden by DIRECT GRANT READ
			case CANCEL_INHERITED:
				for ( NTFSAccessControlEntry p0 : applied_perms ){
					if ( p0.target == ENTFSACETarget.INHERIT && p0.action == ENTFSACEAction.DENY) {
						for ( NTFSAccessControlEntry p2 : applied_perms ) {
							if ( p2.perm == p0.perm && p2.action == ENTFSACEAction.GRANT && p2.target == ENTFSACETarget.DIRECT ) {
								
								if (log_pw!=null)
									log_pw.println("SKIP: DIRECT GRANT "+p0.perm+" ALWAYS OVERRIDES AN INHERITED DENY "+p0.perm);
								
								return;
							}
						}
					}
				} // end for
				break;
			} // end switch

			// This perm set is an affector.
			String setup_target = null;
			if( do_setup ) {
				setup_target = childACL.generate(host);
			}

			callback_function.permutation( setup_target, childACL, callback_arguments );

			if( do_setup ) {
				childACL.destroy(host);
			}
		} // end if (av_perms.length > 0)
	} // end protected void _permute
	
	public interface IPermutationCallback {
		void permutation(String setup_target, NTFSACL childACL, Object callback_arguments);
	}
	
	public static void doGrant(Host host, String path, NTFSAccessControlEntry entry) throws Exception {
		doGrant(host, path, entry, false);
	}
	
	public static void doGrant(Host host, String path, NTFSAccessControlEntry entry, boolean recursive) throws Exception {
		doGrant(host, path, entry.perm, entry.user, recursive);
	}
	
	public static void doGrant(Host host, String path, ENTFSACEType perm) throws Exception {
		doGrant(host, path, perm, EVERYONE);
	}
	
	public static void doGrant(Host host, String path, ENTFSACEType perm, String for_user) throws Exception {
		doGrant(host, path, perm, for_user, false);
	}
	
	public static void doGrant(Host host, String path, ENTFSACEType perm, String forUser, boolean recursive) throws Exception {
		if(forUser==null)
			forUser = EVERYONE;
			
		setACL(host, path, "file", "ace", "n:"+forUser+";m:grant;p:"+perm, ( recursive ? "cont_obj" : "no"));
	}
	
	public static void doDeny(Host host, String path, NTFSAccessControlEntry entry) throws Exception {
		doDeny(host, path, entry, false);
	}
	
	public static void doDeny(Host host, String path, NTFSAccessControlEntry entry, boolean recursive) throws Exception {
		doDeny(host, path, entry.perm, entry.user, recursive);
	}
	
	public static void doDeny(Host host, String path, ENTFSACEType perm) throws Exception {
		doDeny(host, path, perm, EVERYONE);
	}
	
	public static void doDeny(Host host, String path, ENTFSACEType perm, String for_user) throws Exception {
		doDeny(host, path, perm, for_user, false);
	}
	
	public static void doDeny(Host host, String path, ENTFSACEType perm, String forUser, boolean recursive) throws Exception {
		if (forUser==null)
			forUser = EVERYONE;
		
		setACL(host, path, "file", "ace", "n:"+forUser+";m:deny;p:"+perm, ( recursive ? "cont_obj" : "no" ));
	}
	
	public static void doClear(Host host, String path) throws Exception {
		doClear(host, path, false);
	}
	
	public static void doClear(Host host, String path, boolean recursive) throws Exception {
								
		// clear target item (remove the grant full)
		// setacl -on filesdir/parentdir/existing_file -ot file -actn clear -clr "dacl,sacl"
		
		setACL(host, path, "file", "clear", "dacl,sacl", recursive ? "cont_obj" : "no");
	}
	
	public static void doBlock(Host host, String path) throws Exception {
		setACL(host, path, "file", "setprot", "dacl:p_nc;sacl:p_nc", "cont_obj");
	}
	
	protected static void setACL(Host host, String on, String ot, String actn, String op, String rec) throws Exception {
		setACL(host, " -on \""+on+"\" -ot \""+ot+"\" -actn \""+actn+"\" -op \""+op+"\" -rec \""+rec+"\"");
	}
	
	protected static void setACL(Host host, String args) throws Exception {
		//WL( '\t' + 'setacl' + args );
		host.exec(host.getPfttBinDir()+"/setacl " + args, Host.FOUR_HOURS );
	}
	
} // end public class NTFSACL
