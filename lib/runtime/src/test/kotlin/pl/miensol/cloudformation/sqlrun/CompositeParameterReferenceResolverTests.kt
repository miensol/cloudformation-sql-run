package pl.miensol.cloudformation.sqlrun

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pl.miensol.shouldko.shouldEqual

internal class CompositeParameterReferenceResolverTests {

    @Test
    fun `passes value to every resolver`() {
        //given
        val sut = CompositeParameterReferenceResolver(
            listOf(
                mockk() { every { resolve("abc") } returns "123" },
                mockk() { every { resolve("123") } returns "resolved" },
            )
        )


        //when
        val resolved = sut.resolve("abc")

        //then
        resolved.shouldEqual("resolved")
    }
}