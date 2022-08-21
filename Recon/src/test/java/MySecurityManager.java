import java.security.Permission;

class MySecurityException extends SecurityException {
    public int status;
    MySecurityException(int status) {
        this.status = status;
    }
}

class MySecurityManager extends SecurityManager {
    @Override public void checkExit(int status) {
        System.out.println("STATUS");
        System.out.println(status);
        throw new MySecurityException(status);
    }

    @Override public void checkPermission(Permission perm) {
        // Allow other activities by default
    }
}