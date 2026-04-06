package mapper;

import model.UserEntity;
import model.UserCreateDTO;
import model.UserResponseDTO;
import model.UserUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    UserEntity toEntity(UserCreateDTO dto);

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToString")
    UserResponseDTO toResponseDTO(UserEntity userEntity);

    @Mapping(target = "id", source = "existingUser.id")
    @Mapping(target = "createdAt", source = "existingUser.createdAt")
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "email", source = "dto.email")
    @Mapping(target = "age", source = "dto.age")
    UserEntity updateEntity(UserUpdateDTO dto, UserEntity existingUser);

    @Named("instantToString")
    default String instantToString(Instant instant) {
        return instant == null ? "—" : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}

