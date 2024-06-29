package cn.huspace.temperature.temp.api;

import cn.huspace.temperature.temp.utils.SshUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eu")
public class EuMachineApi {

    private final SshUtil sshUtil;

    @Autowired
    public EuMachineApi(SshUtil sshUtil) {
        this.sshUtil = sshUtil;
    }

    @GetMapping(path = "/register")
    public String getAllDevices() {
        return sshUtil.getEuSshResult("s5.serv00.com", 22, "sunwenjie", "fDm7DYK#lPrI%^2RCSah").toString();
    }
}
