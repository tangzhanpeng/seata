/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.console.controller;

import javax.annotation.Resource;

import io.seata.core.console.param.GlobalSessionParam;
import io.seata.core.console.result.PageResult;
import io.seata.core.console.vo.GlobalSessionVO;
import io.seata.server.console.service.GlobalSessionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global Session Controller
 * @author: zhongxiang.wang
 */
@RestController
@RequestMapping("console/globalSession")
public class GlobalSessionController {

    @Resource(type = GlobalSessionService.class)
    private GlobalSessionService globalSessionService;

    /**
     * Query all globalSession
     * @param param param for query globalSession
     * @return  the list of GlobalSessionVO
     */
    @PostMapping("query")
    public PageResult<GlobalSessionVO> query(@RequestBody GlobalSessionParam param) {
        return globalSessionService.query(param);
    }
}
