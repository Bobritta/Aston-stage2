package mapper;

import model.UserEntity;
import model.UserCreateDTO;
import model.UserResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserEntity toEntity(UserCreateDTO dto);

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToString")
    UserResponseDTO toResponseDTO(UserEntity userEntity);

    @Named("instantToString")
    default String instantToString(Instant instant) {
        return instant == null ? "—" : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}

