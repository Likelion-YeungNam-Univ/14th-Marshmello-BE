package Marshmello.MarshmelloWas.domain.checkin.port;

import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInCommand;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInView;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;

public interface CheckInStorePort {

    CheckInView save(CheckInCommand command, short score);
}
