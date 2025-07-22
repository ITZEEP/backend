package org.scoula.global.common.exception;

/** 엔티티를 찾을 수 없을 때 발생하는 예외 */
public class EntityNotFoundException extends BaseException {

      public EntityNotFoundException(String message) {
          super(CommonErrorCode.ENTITY_NOT_FOUND, message);
      }

      public EntityNotFoundException() {
          super(CommonErrorCode.ENTITY_NOT_FOUND);
      }
}
