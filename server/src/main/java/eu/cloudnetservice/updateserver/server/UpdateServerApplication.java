/*
 * Licensed to CloudNetService under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cloudnetservice.updateserver.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * @author derklaro
 * @version 1.0
 * @since 2. October 2020
 */
@SpringBootApplication
public class UpdateServerApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(UpdateServerApplication.class);
        springApplication.setDefaultProperties(Map.of(
            // logging
            "logging.group.tomcat", "org.apache.catalina, org.apache.coyote, org.apache.tomcat",
            "logging.level.tomcat", "OFF"
        ));

        springApplication.run(args);
    }
}
