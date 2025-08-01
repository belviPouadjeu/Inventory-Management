package com.belvinard.gestionstock.service.impl;

import com.belvinard.gestionstock.dto.ChangerMotDePasseUtilisateurDTO;
import com.belvinard.gestionstock.dto.UtilisateurDTO;
import com.belvinard.gestionstock.models.Entreprise;
import com.belvinard.gestionstock.models.RoleType;
import com.belvinard.gestionstock.models.Roles;
import com.belvinard.gestionstock.models.Utilisateur;
import com.belvinard.gestionstock.repositories.EntrepriseRepository;
import com.belvinard.gestionstock.repositories.RolesRepository;
import com.belvinard.gestionstock.repositories.UtilisateurRepository;
import com.belvinard.gestionstock.service.UtilisateurService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final RolesRepository rolesRepository;
    private final ModelMapper modelMapper;

    @Override
    public UtilisateurDTO save(UtilisateurDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("UtilisateurDTO ne peut pas être null");
        }

        // Vérifier si l'email existe déjà
        if (utilisateurRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("Un utilisateur avec cet email existe déjà");
        }

        // Vérifier que l'entreprise existe
        Entreprise entreprise = entrepriseRepository.findById(dto.getEntreprise().getId())
                .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée"));

        Utilisateur utilisateur = modelMapper.map(dto, Utilisateur.class);
        utilisateur.setEntreprise(entreprise);

        utilisateur = utilisateurRepository.save(utilisateur);
        return modelMapper.map(utilisateur, UtilisateurDTO.class);
    }

    @Override
    public UtilisateurDTO findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant est obligatoire");
        }

        return utilisateurRepository.findById(id.longValue())
                .map(user -> modelMapper.map(user, UtilisateurDTO.class))
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
    }

    @Override
    public List<UtilisateurDTO> findAll() {
        return utilisateurRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UtilisateurDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant est obligatoire");
        }

        if (!utilisateurRepository.existsById(id.longValue())) {
            throw new EntityNotFoundException("Utilisateur non trouvé");
        }

        utilisateurRepository.deleteById(id.longValue());
    }

    @Override
    public UtilisateurDTO findByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }

        return utilisateurRepository.findByEmail(email)
                .map(user -> modelMapper.map(user, UtilisateurDTO.class))
                .orElseThrow(() -> new EntityNotFoundException("Aucun utilisateur avec cet email"));
    }

    @Override
    public UtilisateurDTO changerMotDePasse(ChangerMotDePasseUtilisateurDTO dto) {
        if (dto == null || dto.getId() == null || dto.getMotDePasseActuel() == null || dto.getNouveauMotDePasse() == null) {
            throw new IllegalArgumentException("Les informations de changement de mot de passe sont incomplètes");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        if (!utilisateur.getMoteDePasse().equals(dto.getMotDePasseActuel())) {
            throw new IllegalStateException("Ancien mot de passe incorrect");
        }

        utilisateur.setMoteDePasse(dto.getNouveauMotDePasse());
        utilisateur = utilisateurRepository.save(utilisateur);

        return modelMapper.map(utilisateur, UtilisateurDTO.class);
    }

    @Override
    public UtilisateurDTO findByIdLonge(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant est obligatoire");
        }

        return utilisateurRepository.findById(id)
                .map(user -> modelMapper.map(user, UtilisateurDTO.class))
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
    }

    // === NOUVELLES MÉTHODES ===

    @Override
    public UtilisateurDTO assignRole(Long userId, RoleType roleType) {
        if (userId == null || roleType == null) {
            throw new IllegalArgumentException("L'ID utilisateur et le type de rôle sont obligatoires");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        // Vérifier si l'utilisateur a déjà ce rôle
        Optional<Roles> existingRole = rolesRepository.findByUtilisateurIdAndRoleType(userId, roleType);
        if (existingRole.isPresent()) {
            throw new IllegalStateException("L'utilisateur possède déjà ce rôle");
        }

        // Créer et assigner le nouveau rôle
        Roles nouveauRole = new Roles();
        nouveauRole.setRoleType(roleType);
        nouveauRole.setUtilisateur(utilisateur);
        nouveauRole.setRoleName(roleType.name()); // Ajout du nom du rôle

        rolesRepository.save(nouveauRole);

        return modelMapper.map(utilisateur, UtilisateurDTO.class);
    }
    @Override
    public UtilisateurDTO removeRole(Long userId, RoleType roleType) {
        if (userId == null || roleType == null) {
            throw new IllegalArgumentException("L'ID utilisateur et le type de rôle sont obligatoires");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        Roles roleToRemove = utilisateur.getRoles().stream()
                .filter(role -> role.getRoleType() == roleType)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("L'utilisateur ne possède pas ce rôle"));

        utilisateur.getRoles().remove(roleToRemove);
        rolesRepository.delete(roleToRemove);

        return modelMapper.map(utilisateur, UtilisateurDTO.class);
    }

    @Override
    public List<UtilisateurDTO> findByRole(RoleType roleType) {
        if (roleType == null) {
            throw new IllegalArgumentException("Le type de rôle est obligatoire");
        }

        return utilisateurRepository.findByRoles_RoleType(roleType).stream()
                .map(user -> modelMapper.map(user, UtilisateurDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public UtilisateurDTO activateUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("L'ID utilisateur est obligatoire");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        utilisateur.setActif(true); // ou le nom de votre méthode setter
        utilisateur = utilisateurRepository.save(utilisateur);

        return modelMapper.map(utilisateur, UtilisateurDTO.class);
    }

    @Override
    public UtilisateurDTO deactivateUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("L'ID utilisateur est obligatoire");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        utilisateur.setActif(false); // ou le nom de votre méthode setter
        utilisateur = utilisateurRepository.save(utilisateur);

        return modelMapper.map(utilisateur, UtilisateurDTO.class);
    }

    @Override
    public List<UtilisateurDTO> findByEntreprise(Long entrepriseId) {
        if (entrepriseId == null) {
            throw new IllegalArgumentException("L'ID de l'entreprise est obligatoire");
        }

        return utilisateurRepository.findByEntrepriseId(entrepriseId).stream()
                .map(user -> modelMapper.map(user, UtilisateurDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<UtilisateurDTO> findActiveUsers() {
        return utilisateurRepository.findByActifTrue().stream()
                .map(user -> modelMapper.map(user, UtilisateurDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<UtilisateurDTO> findInactiveUsers() {
        return utilisateurRepository.findByActifFalse().stream()
                .map(user -> modelMapper.map(user, UtilisateurDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<UtilisateurDTO> findActiveUsersByEntreprise(Long entrepriseId) {
        if (entrepriseId == null) {
            throw new IllegalArgumentException("L'ID de l'entreprise est obligatoire");
        }

        return utilisateurRepository.findByEntrepriseIdAndActifTrue(entrepriseId).stream()
                .map(user -> modelMapper.map(user, UtilisateurDTO.class))
                .collect(Collectors.toList());
    }
}