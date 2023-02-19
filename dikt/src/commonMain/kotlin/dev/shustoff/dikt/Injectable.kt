package dev.shustoff.dikt

/**
 * When class directly implements this interface compiler plugin will be able to create instances of this class when replacing `resolve()` calls
 * This doesn't require any additional annotations around code that uses `resolve()` function
 */
interface Injectable