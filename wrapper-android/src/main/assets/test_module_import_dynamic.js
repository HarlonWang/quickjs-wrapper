import {assertEquals} from "./test_assert_define.js";

// dynamic import
import("test_base_module2.mjs").then((res) => {
    assertEquals(res.name, "Jack")
    assertEquals(res.age, 18)
})
