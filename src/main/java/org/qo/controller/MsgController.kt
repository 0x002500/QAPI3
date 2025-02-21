package org.qo.controller

import jakarta.servlet.http.HttpServletRequest
import org.qo.exception.ReturnInterface
import org.qo.server.Nodes
import org.qo.service.Msg
import org.qo.util.UAUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class MsgController @Autowired constructor(
    val ua: UAUtil,
    val nodes: Nodes,
    val ri: ReturnInterface
) {
    @PostMapping("/qo/msglist/upload")
    fun handleMsg(@RequestBody data: String, request: HttpServletRequest): ResponseEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        return if (ua.isCLIToolRequest(request)) {
            ResponseEntity("failed", headers, HttpStatus.BAD_REQUEST)
        } else {
            if (nodes.validate_message(data)) {
                ResponseEntity("success", headers, HttpStatus.OK)
            } else {
                ResponseEntity("failed", headers, HttpStatus.BAD_REQUEST)
            }
        }
    }

    @GetMapping("/qo/msglist/download")
    fun returnMsg(): ResponseEntity<String> {
        return ri.GeneralHttpHeader(Msg.get().toString())
    }
    @GetMapping("/qo/msglist/sse")
    suspend fun sseConnection(): SseEmitter {
        return Msg.sse()
    }
}
