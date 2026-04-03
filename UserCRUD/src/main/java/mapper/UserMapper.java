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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    UserEntity updateEntity(@MappingTarget UserEntity user, UserUpdateDTO dto);

    @Named("instantToString")
    default String instantToString(Instant instant) {
        return instant == null ? "—" : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}

