package com.gffh.mobile.core.image

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIRectFill
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * PHPickerViewController, presented from the key window's root view
 * controller (the same way [com.gffh.mobile.core.platform.saveTextFile]
 * presents the share sheet). It runs out of process, so no Photos
 * permission - and no NSPhotoLibraryUsageDescription - is needed.
 */
@Composable
actual fun rememberBadgeImagePicker(onResult: (BadgePickResult) -> Unit): () -> Unit = { presentBadgePicker(onResult) }

/**
 * UIKit holds its delegate weakly, so the delegate is kept here for as long
 * as the picker is on screen. Only ever touched on the main thread.
 */
private var activeDelegate: BadgePickerDelegate? = null

private fun presentBadgePicker(onResult: (BadgePickResult) -> Unit) {
    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
    if (rootViewController == null) {
        onResult(BadgePickResult.Failed)
        return
    }
    val configuration = PHPickerConfiguration().apply {
        selectionLimit = 1L
        filter = PHPickerFilter.imagesFilter
    }
    val delegate = BadgePickerDelegate(onResult)
    activeDelegate = delegate
    val picker = PHPickerViewController(configuration = configuration)
    picker.delegate = delegate
    rootViewController.presentViewController(picker, animated = true, completion = null)
}

private class BadgePickerDelegate(private val onResult: (BadgePickResult) -> Unit) :
    NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val itemProvider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
        if (itemProvider == null) {
            finish(BadgePickResult.Cancelled)
            return
        }
        // Loading runs off the main thread and may answer on any thread.
        itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data: NSData?, _ ->
            val dataUrl = data?.let { badgeDataUrl(it) }
            val result = if (dataUrl == null) BadgePickResult.Failed else BadgePickResult.Picked(dataUrl)
            dispatch_async(dispatch_get_main_queue()) { finish(result) }
        }
    }

    private fun finish(result: BadgePickResult) {
        activeDelegate = null
        onResult(result)
    }
}

/** Square centre crop, at most [BADGE_MAX_SIZE_PX] a side, JPEG, as a data URL - same output as gffh-web's BadgePicker. */
@OptIn(ExperimentalForeignApi::class)
private fun badgeDataUrl(data: NSData): String? {
    val image = UIImage.imageWithData(data) ?: return null
    val width = image.size.useContents { width }
    val height = image.size.useContents { height }
    if (width <= 0.0 || height <= 0.0) return null

    val shortestSide = minOf(width, height)
    val target = minOf(BADGE_MAX_SIZE_PX.toDouble(), shortestSide)
    // Scale 1.0: the square is in pixels, not points, so a Retina device
    // doesn't produce a 512px badge.
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(target, target), true, 1.0)
    // JPEG has no transparency: a see-through PNG gets a white background,
    // as it does on the web, rather than a black one.
    UIColor.whiteColor.setFill()
    UIRectFill(CGRectMake(0.0, 0.0, target, target))
    val scale = target / shortestSide
    val drawWidth = width * scale
    val drawHeight = height * scale
    image.drawInRect(CGRectMake((target - drawWidth) / 2, (target - drawHeight) / 2, drawWidth, drawHeight))
    val square = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    val jpeg = square?.let { UIImageJPEGRepresentation(it, BADGE_JPEG_QUALITY) } ?: return null
    return BADGE_DATA_URL_PREFIX + jpeg.base64EncodedStringWithOptions(0uL)
}
