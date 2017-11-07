package pasta.domain.reporting;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import pasta.domain.UserPermissionLevel;

@Entity
@Table(name = "reports")
public class Report {
	
	@Id
	private String id;
	
	@Column(nullable = false)
	private String name;
	
	@Column
	private String description;
	
	@ElementCollection(targetClass = UserPermissionLevel.class, fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	@CollectionTable (name = "report_default_permissions", joinColumns=@JoinColumn(name = "report_id"))
	@Column(name = "permission", nullable = false)
	@Fetch (FetchMode.SELECT)
	private Set<UserPermissionLevel> defaultPermissions;
	
	@OneToMany(
			mappedBy = "id.report", 
			cascade = CascadeType.ALL, 
			orphanRemoval = true
	)
	@LazyCollection(LazyCollectionOption.FALSE)
	private Set<ReportPermission> permissions;
	
	public Report() {
		
	}
	
	public Report(String id, String name, String description, UserPermissionLevel... defaultPermissions) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.defaultPermissions = new HashSet<>();
		for(UserPermissionLevel permission : defaultPermissions) {
			addDefaultPermissionLevel(permission);
		}
		this.permissions = new HashSet<>();
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public Set<UserPermissionLevel> getDefaultPermissionLevels() {
		return defaultPermissions;
	}

	public void setDefaultPermissionLevels(Collection<UserPermissionLevel> permissionLevels) {
		this.defaultPermissions.clear();
		if(permissionLevels != null) {
			this.defaultPermissions.addAll(permissionLevels);
		}
	}
	
	public boolean addDefaultPermissionLevel(UserPermissionLevel permission) {
		return this.defaultPermissions.add(permission);
	}
	public boolean removeDefaultPermissionLevel(UserPermissionLevel permission) {
		return this.defaultPermissions.remove(permission);
	}
	
	public Set<ReportPermission> getPermissions() {
		return permissions;
	}
	public void setPermissions(Set<ReportPermission> permissions) {
		for(ReportPermission permission : this.permissions) {
			permission.setReport(null);
		}
		this.permissions.clear();
		for(ReportPermission permission : permissions) {
			addPermission(permission);
		}
	}
	public void addPermission(ReportPermission permission) {
		permissions.add(permission);
		permission.setReport(this);
	}
	public void removePermission(ReportPermission permission) {
		permissions.remove(permission);
		permission.setReport(null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Report other = (Report) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
