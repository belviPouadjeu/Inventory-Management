package com.belvinard.gestionstock.repositories;


import com.belvinard.gestionstock.models.RoleType;
import com.belvinard.gestionstock.models.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolesRepository extends JpaRepository<Roles, Long> {

    List<Roles> findByRoleType(RoleType roleType);
    List<Roles> findByUtilisateurId(Long utilisateurId);
    Optional<Roles> findByUtilisateurIdAndRoleType(Long utilisateurId, RoleType roleType);
    void deleteByUtilisateurIdAndRoleType(Long utilisateurId, RoleType roleType);
}