package vn.tayjava.service.Implement;

import ch.qos.logback.core.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.tayjava.common.UserStatus;
import vn.tayjava.controller.request.UserCreationRequest;
import vn.tayjava.controller.request.UserPasswordRequest;
import vn.tayjava.controller.request.UserUpdateRequest;
import vn.tayjava.controller.response.UserPageResponse;
import vn.tayjava.controller.response.UserResponse;
import vn.tayjava.exception.ResourceNotFoundException;
import vn.tayjava.model.AddressEntity;
import vn.tayjava.model.UserEntity;
import vn.tayjava.repository.AddressRepository;
import vn.tayjava.repository.UserRepository;
import vn.tayjava.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j(topic = "USER-SERVICE")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserPageResponse findAll(String keyword, String sort, int page, int size) {
        log.info("findAll start");

        //Sorting
        Sort.Order order = new Sort.Order(Sort.Direction.ASC, "id");
        if (StringUtils.hasLength(sort)) {
            Pattern pattern = Pattern.compile("(\\w+?)(:)(.*)"); // tencot:asc|desc
            Matcher matcher = pattern.matcher(sort);
            if (matcher.find()) {
                String columnName = matcher.group(1);
                if (matcher.group(3).equalsIgnoreCase("asc")) {
                    order = new Sort.Order(Sort.Direction.ASC, columnName);
                } else {
                    order = new Sort.Order(Sort.Direction.DESC, columnName);
                }
            }
        }

        // Xu ly truong hop FE muon bat dau voi page = 1
        int pageNo = 0;
        if (page > 0) {
            pageNo = page - 1;
        }

        // Paging
        Pageable pageable = PageRequest.of(pageNo, size, Sort.by(order));

        Page<UserEntity> entityPage;

        if (StringUtils.hasLength(keyword)) {
            keyword = "%" + keyword.toLowerCase() + "%";
            entityPage = userRepository.searchByKeyword(keyword, pageable);
        } else {
            entityPage = userRepository.findAll(pageable);
        }

        return getUserPageResponse(page, size, entityPage);
    }

    @Override
    public UserResponse findById(Long id) {
        log.info("Find user by id: {}", id);

        UserEntity userEntity = getUserEntity(id);

        return UserResponse.builder()
                .id(id)
                .firstName(userEntity.getFirstName())
                .lastName(userEntity.getLastName())
                .gender(userEntity.getGender())
                .birthday(userEntity.getBirthday())
                .username(userEntity.getUsername())
                .phone(userEntity.getPhone())
                .email(userEntity.getEmail())
                .build();
    }

    @Override
    public UserResponse findByUsername(String username) {
        return null;
    }

    @Override
    public UserResponse findByEmail(String email) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(UserCreationRequest req) {
        log.info("Saving user: {}", req);
        UserEntity user = new UserEntity();
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setGender(req.getGender());
        user.setBirthday(req.getBirthday());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setUsername(req.getUsername());
        user.setType(req.getType());
        user.setStatus(UserStatus.NONE);
        userRepository.save(user);
        log.info("Saved user: {}", user);

        if (user.getId() != null) {
            log.info("user id: {}", user.getId());
            List<AddressEntity> addresses = new ArrayList<>();
            req.getAddresses().forEach(address -> {
                AddressEntity addressEntity = new AddressEntity();
                addressEntity.setApartmentNumber(address.getApartmentNumber());
                addressEntity.setFloor(address.getFloor());
                addressEntity.setBuilding(address.getBuilding());
                addressEntity.setStreetNumber(address.getStreetNumber());
                addressEntity.setStreet(address.getStreet());
                addressEntity.setCity(address.getCity());
                addressEntity.setCountry(address.getCountry());
                addressEntity.setAddressType(address.getAddressType());
                addressEntity.setUserId(user.getId());
                addresses.add(addressEntity);
            });
            addressRepository.saveAll(addresses);
            log.info("Saved addresses: {}", addresses);
        }

        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateRequest req) {
        log.info("Updating user: {}", req);

        // Get user by id
        UserEntity user = getUserEntity(req.getId());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setGender(req.getGender());
        user.setBirthday(req.getBirthday());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setUsername(req.getUsername());

        userRepository.save(user);
        log.info("Updated user: {}", user);

        // save address
//        List<AddressEntity> addresses = new ArrayList<>();
//
//        req.getAddresses().forEach(address -> {
//            AddressEntity addressEntity = addressRepository.findByUserIdAndAddressType(user.getId(), address.getAddressType());
//            if (addressEntity == null) {
//                addressEntity = new AddressEntity();
//            }
//            addressEntity.setApartmentNumber(address.getApartmentNumber());
//            addressEntity.setFloor(address.getFloor());
//            addressEntity.setBuilding(address.getBuilding());
//            addressEntity.setStreetNumber(address.getStreetNumber());
//            addressEntity.setStreet(address.getStreet());
//            addressEntity.setCity(address.getCity());
//            addressEntity.setCountry(address.getCountry());
//            addressEntity.setAddressType(address.getAddressType());
//            addressEntity.setUserId(user.getId());
//
//            addresses.add(addressEntity);
//        });

        List<AddressEntity> addresses = new ArrayList<>();
        req.getAddresses().forEach(address -> {
            AddressEntity addressEntity = addressRepository.findByUserIdAndAddressType(user.getId(), address.getAddressType());
            if(addressEntity == null) {
                addressEntity = new AddressEntity();
            }
            addressEntity.setApartmentNumber(address.getApartmentNumber());
            addressEntity.setFloor(address.getFloor());
            addressEntity.setBuilding(address.getBuilding());
            addressEntity.setStreetNumber(address.getStreetNumber());
            addressEntity.setStreet(address.getStreet());
            addressEntity.setCity(address.getCity());
            addressEntity.setCountry(address.getCountry());
            addressEntity.setAddressType(address.getAddressType());
            addressEntity.setUserId(user.getId());

            addresses.add(addressEntity);
        });

        // save addresses
        addressRepository.saveAll(addresses);
        log.info("Updated addresses: {}", addresses);
    }

    @Override
    public void changePassword(UserPasswordRequest req) {
        log.info("Changing password for user: {}", req);

        // Get user by id
        UserEntity user = getUserEntity(req.getId());
        if (req.getPassword().equals(req.getConfirmPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            userRepository.save(user);
            log.info("Changed password for user: {}", user);
        }

        log.error("Changed password failed");

    }

    @Override
    public void delete(Long id) {
        log.info("Deleting user: {}", id);

        // Get user by id
        UserEntity user = getUserEntity(id);
        user.setStatus(UserStatus.INACTIVE);

        userRepository.save(user);
        log.info("Deleted user id: {}", id);
    }

    /**
     * Get user by id
     *
     * @param id
     * @return
     */
    private UserEntity getUserEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Convert EserEntities to UserResponse
     *
     * @param page
     * @param size
     * @param userEntities
     * @return UserPageResponse
     */
    private static UserPageResponse getUserPageResponse(int page, int size, Page<UserEntity> userEntities) {
        log.info("Convert User Entity Page");

        List<UserResponse> userList = userEntities.stream().map(entity -> UserResponse.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .gender(entity.getGender())
                .birthday(entity.getBirthday())
                .username(entity.getUsername())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .build()
        ).toList();

        UserPageResponse response = new UserPageResponse();
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setTotalElements(userEntities.getTotalElements());
        response.setTotalPages(userEntities.getTotalPages());
        response.setUsers(userList);

        return response;
    }
}
