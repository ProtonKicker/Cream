package ru.ytkab0bp.beamklipper.view

import android.animation.TimeInterpolator
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.view.ViewCompat
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.utils.ViewUtils
import java.util.ArrayList

class SmoothItemAnimator : SimpleItemAnimator() {
    private val mPendingRemovals = ArrayList<RecyclerView.ViewHolder>()
    private val mPendingAdditions = ArrayList<RecyclerView.ViewHolder>()
    private val mPendingMoves = ArrayList<MoveInfo>()
    private val mPendingChanges = ArrayList<ChangeInfo>()

    internal val mAdditionsList = ArrayList<ArrayList<RecyclerView.ViewHolder>>()
    internal val mMovesList = ArrayList<ArrayList<MoveInfo>>()
    internal val mChangesList = ArrayList<ArrayList<ChangeInfo>>()

    internal val mAddAnimations = ArrayList<RecyclerView.ViewHolder>()
    internal val mMoveAnimations = ArrayList<RecyclerView.ViewHolder>()
    internal val mRemoveAnimations = ArrayList<RecyclerView.ViewHolder>()
    internal val mChangeAnimations = ArrayList<RecyclerView.ViewHolder>()

    internal class MoveInfo(
        var holder: RecyclerView.ViewHolder,
        var fromX: Int, var fromY: Int,
        var toX: Int, var toY: Int
    )

    internal class ChangeInfo(
        var oldHolder: RecyclerView.ViewHolder?,
        var newHolder: RecyclerView.ViewHolder?,
        var fromX: Int = 0, var fromY: Int = 0,
        var toX: Int = 0, var toY: Int = 0
    ) {
        constructor(oldHolder: RecyclerView.ViewHolder?, newHolder: RecyclerView.ViewHolder?) : this(oldHolder, newHolder, 0, 0, 0, 0)
    }

    override fun runPendingAnimations() {
        val removalsPending = mPendingRemovals.isNotEmpty()
        val movesPending = mPendingMoves.isNotEmpty()
        val changesPending = mPendingChanges.isNotEmpty()
        val additionsPending = mPendingAdditions.isNotEmpty()
        if (!removalsPending && !movesPending && !additionsPending && !changesPending) return

        for (holder in mPendingRemovals) {
            animateRemoveImpl(holder)
        }
        mPendingRemovals.clear()

        if (movesPending) {
            val moves = ArrayList<MoveInfo>().apply { addAll(mPendingMoves) }
            mMovesList.add(moves)
            mPendingMoves.clear()
            val mover = Runnable {
                for (moveInfo in moves) {
                    animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY, moveInfo.toX, moveInfo.toY)
                }
                moves.clear()
                mMovesList.remove(moves)
            }
            if (removalsPending) {
                ViewCompat.postOnAnimationDelayed(moves[0].holder.itemView, mover, removeDuration)
            } else {
                mover.run()
            }
        }

        if (changesPending) {
            val changes = ArrayList<ChangeInfo>().apply { addAll(mPendingChanges) }
            mChangesList.add(changes)
            mPendingChanges.clear()
            val changer = Runnable {
                for (change in changes) {
                    animateChangeImpl(change)
                }
                changes.clear()
                mChangesList.remove(changes)
            }
            if (removalsPending) {
                changes[0].oldHolder?.itemView?.let { ViewCompat.postOnAnimationDelayed(it, changer, removeDuration) }
                    ?: return
            } else {
                changer.run()
            }
        }

        if (additionsPending) {
            val additions = ArrayList<RecyclerView.ViewHolder>().apply { addAll(mPendingAdditions) }
            mAdditionsList.add(additions)
            mPendingAdditions.clear()
            val adder = Runnable {
                for (holder in additions) {
                    animateAddImpl(holder)
                }
                additions.clear()
                mAdditionsList.remove(additions)
            }
            if (removalsPending || movesPending || changesPending) {
                val totalDelay = removeDuration + Math.max(moveDuration, changeDuration)
                ViewCompat.postOnAnimationDelayed(additions[0].itemView, adder, totalDelay)
            } else {
                adder.run()
            }
        }
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        mPendingRemovals.add(holder)
        return true
    }

    private fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        val spring = SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1 / 256f)
            .setSpring(SpringForce(1f).setStiffness(1000f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
            .addUpdateListener { _, value, _ -> view.alpha = 1f - value }
            .addEndListener { _, canceled, _, _ ->
                if (!canceled) view.alpha = 1f
                dispatchRemoveFinished(holder)
                mRemoveAnimations.remove(holder)
                dispatchFinishedWhenDone()
            }
        view.setTag(R.id.spring, spring)
        mRemoveAnimations.add(holder)
        dispatchRemoveStarting(holder)
        spring.start()
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        holder.itemView.alpha = 0f
        mPendingAdditions.add(holder)
        return true
    }

    internal fun animateAddImpl(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        val spring = SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1 / 256f)
            .setSpring(SpringForce(1f).setStiffness(1000f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
            .addUpdateListener { _, value, _ -> view.alpha = value }
            .addEndListener { _, canceled, _, _ ->
                if (canceled) view.alpha = 1f
                dispatchAddFinished(holder)
                mAddAnimations.remove(holder)
                dispatchFinishedWhenDone()
            }
        view.setTag(R.id.spring, spring)
        mAddAnimations.add(holder)
        dispatchAddStarting(holder)
        spring.start()
    }

    override fun animateMove(holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        val view = holder.itemView
        val fromX2 = fromX + view.translationX.toInt()
        val fromY2 = fromY + view.translationY.toInt()
        resetAnimation(holder)
        val deltaX = toX - fromX2
        val deltaY = toY - fromY2
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder)
            return false
        }
        if (deltaX != 0) view.translationX = -deltaX.toFloat()
        if (deltaY != 0) view.translationY = -deltaY.toFloat()
        mPendingMoves.add(MoveInfo(holder, fromX2, fromY2, toX, toY))
        return true
    }

    internal fun animateMoveImpl(holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int) {
        val view = holder.itemView
        val fX = view.translationX
        val fY = view.translationY
        val spring = SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1 / 256f)
            .setSpring(SpringForce(1f).setStiffness(1000f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
            .addUpdateListener { _, value, _ ->
                view.translationX = ViewUtils.lerp(fX, 0f, value)
                view.translationY = ViewUtils.lerp(fY, 0f, value)
            }
            .addEndListener { _, canceled, _, _ ->
                if (canceled) {
                    view.translationX = 0f
                    view.translationY = 0f
                }
                dispatchMoveFinished(holder)
                mMoveAnimations.remove(holder)
                dispatchFinishedWhenDone()
            }
        view.setTag(R.id.spring, spring)
        mMoveAnimations.add(holder)
        dispatchMoveStarting(holder)
        spring.start()
    }

    override fun animateChange(oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder?, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        if (oldHolder === newHolder) {
            return animateMove(oldHolder, fromX, fromY, toX, toY)
        }
        val prevTranslationX = oldHolder.itemView.translationX
        val prevTranslationY = oldHolder.itemView.translationY
        val prevAlpha = oldHolder.itemView.alpha
        resetAnimation(oldHolder)
        val deltaX = (toX - fromX - prevTranslationX).toInt()
        val deltaY = (toY - fromY - prevTranslationY).toInt()
        oldHolder.itemView.translationX = prevTranslationX
        oldHolder.itemView.translationY = prevTranslationY
        oldHolder.itemView.alpha = prevAlpha
        if (newHolder != null) {
            resetAnimation(newHolder)
            newHolder.itemView.translationX = -deltaX.toFloat()
            newHolder.itemView.translationY = -deltaY.toFloat()
            newHolder.itemView.alpha = 0f
        }
        mPendingChanges.add(ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY))
        return true
    }

    internal fun animateChangeImpl(changeInfo: ChangeInfo) {
        val holder = changeInfo.oldHolder
        val view = holder?.itemView
        val newHolder = changeInfo.newHolder
        val newView = newHolder?.itemView

        if (view != null) {
            val fX = view.translationX
            val fY = view.translationY
            val tX = (changeInfo.toX - changeInfo.fromX).toFloat()
            val tY = (changeInfo.toY - changeInfo.fromY).toFloat()
            val spring = SpringAnimation(FloatValueHolder(0f))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(SpringForce(1f).setStiffness(1000f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener { _, value, _ ->
                    view.translationX = ViewUtils.lerp(fX, tX, value)
                    view.translationY = ViewUtils.lerp(fY, tY, value)
                    view.alpha = 1f - value
                }
                .addEndListener { _, canceled, _, _ ->
                    if (!canceled) {
                        view.alpha = 1f
                        view.translationX = 0f
                        view.translationY = 0f
                    }
                    dispatchChangeFinished(changeInfo.oldHolder!!, true)
                    mChangeAnimations.remove(changeInfo.oldHolder)
                    dispatchFinishedWhenDone()
                }
            view.setTag(R.id.spring, spring)
            mChangeAnimations.add(changeInfo.oldHolder!!)
            dispatchChangeStarting(changeInfo.oldHolder!!, true)
            spring.start()
        }

        if (newView != null) {
            val fX = newView.translationX
            val fY = newView.translationY
            val spring = SpringAnimation(FloatValueHolder(0f))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(SpringForce(1f).setStiffness(1000f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener { _, value, _ ->
                    newView.translationX = fX * (1f - value)
                    newView.translationY = fY * (1f - value)
                }
                .addEndListener { _, canceled, _, _ ->
                    if (!canceled) {
                        newView.alpha = 1f
                        newView.translationX = 0f
                        newView.translationY = 0f
                    }
                    dispatchChangeFinished(changeInfo.newHolder!!, false)
                    mChangeAnimations.remove(changeInfo.newHolder)
                    dispatchFinishedWhenDone()
                }
            newView.setTag(R.id.spring, spring)
            mChangeAnimations.add(changeInfo.newHolder!!)
            dispatchChangeStarting(changeInfo.newHolder!!, false)
            spring.start()
        }
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        val view = item.itemView
        val spring = view.getTag(R.id.spring) as? SpringAnimation
        spring?.cancel()

        for (i in mPendingMoves.indices.reversed()) {
            val moveInfo = mPendingMoves[i]
            if (moveInfo.holder === item) {
                view.translationY = 0f
                view.translationX = 0f
                dispatchMoveFinished(item)
                mPendingMoves.removeAt(i)
            }
        }
        endChangeAnimation(mPendingChanges, item)
        if (mPendingRemovals.remove(item)) {
            view.alpha = 1f
            dispatchRemoveFinished(item)
        }
        if (mPendingAdditions.remove(item)) {
            view.alpha = 1f
            dispatchAddFinished(item)
        }

        for (i in mChangesList.indices.reversed()) {
            val changes = mChangesList[i]
            endChangeAnimation(changes, item)
            if (changes.isEmpty()) {
                mChangesList.removeAt(i)
            }
        }
        for (i in mMovesList.indices.reversed()) {
            val moves = mMovesList[i]
            for (j in moves.indices.reversed()) {
                val moveInfo = moves[j]
                if (moveInfo.holder === item) {
                    view.translationY = 0f
                    view.translationX = 0f
                    dispatchMoveFinished(item)
                    moves.removeAt(j)
                    if (moves.isEmpty()) {
                        mMovesList.removeAt(i)
                    }
                    break
                }
            }
        }
        for (i in mAdditionsList.indices.reversed()) {
            val additions = mAdditionsList[i]
            if (additions.remove(item)) {
                view.alpha = 1f
                dispatchAddFinished(item)
                if (additions.isEmpty()) {
                    mAdditionsList.removeAt(i)
                }
            }
        }

        mRemoveAnimations.remove(item)
        mAddAnimations.remove(item)
        mChangeAnimations.remove(item)
        mMoveAnimations.remove(item)

        dispatchFinishedWhenDone()
    }

    private fun resetAnimation(holder: RecyclerView.ViewHolder) {
        endAnimation(holder)
    }

    override fun isRunning(): Boolean {
        return mPendingAdditions.isNotEmpty() || mPendingChanges.isNotEmpty() || mPendingMoves.isNotEmpty() ||
                mPendingRemovals.isNotEmpty() || mMoveAnimations.isNotEmpty() || mRemoveAnimations.isNotEmpty() ||
                mAddAnimations.isNotEmpty() || mChangeAnimations.isNotEmpty() || mMovesList.isNotEmpty() ||
                mAdditionsList.isNotEmpty() || mChangesList.isNotEmpty()
    }

    internal fun dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished()
        }
    }

    override fun endAnimations() {
        var count = mPendingMoves.size
        for (i in count - 1 downTo 0) {
            val item = mPendingMoves[i]
            val view = item.holder.itemView
            view.translationY = 0f
            view.translationX = 0f
            dispatchMoveFinished(item.holder)
            mPendingMoves.removeAt(i)
        }
        count = mPendingRemovals.size
        for (i in count - 1 downTo 0) {
            val item = mPendingRemovals[i]
            dispatchRemoveFinished(item)
            mPendingRemovals.removeAt(i)
        }
        count = mPendingAdditions.size
        for (i in count - 1 downTo 0) {
            val item = mPendingAdditions[i]
            item.itemView.alpha = 1f
            dispatchAddFinished(item)
            mPendingAdditions.removeAt(i)
        }
        count = mPendingChanges.size
        for (i in count - 1 downTo 0) {
            endChangeAnimationIfNecessary(mPendingChanges[i])
        }
        mPendingChanges.clear()
        if (!isRunning()) return

        var listCount = mMovesList.size
        for (i in listCount - 1 downTo 0) {
            val moves = mMovesList[i]
            count = moves.size
            for (j in count - 1 downTo 0) {
                val moveInfo = moves[j]
                val item = moveInfo.holder
                val view = item.itemView
                view.translationY = 0f
                view.translationX = 0f
                dispatchMoveFinished(item)
                moves.removeAt(j)
                if (moves.isEmpty()) {
                    mMovesList.removeAt(i)
                }
            }
        }
        listCount = mAdditionsList.size
        for (i in listCount - 1 downTo 0) {
            val additions = mAdditionsList[i]
            count = additions.size
            for (j in count - 1 downTo 0) {
                val item = additions[j]
                item.itemView.alpha = 1f
                dispatchAddFinished(item)
                additions.removeAt(j)
                if (additions.isEmpty()) {
                    mAdditionsList.removeAt(i)
                }
            }
        }
        listCount = mChangesList.size
        for (i in listCount - 1 downTo 0) {
            val changes = mChangesList[i]
            count = changes.size
            for (j in count - 1 downTo 0) {
                endChangeAnimationIfNecessary(changes[j])
                if (changes.isEmpty()) {
                    mChangesList.removeAt(i)
                }
            }
        }

        cancelAll(mRemoveAnimations)
        cancelAll(mMoveAnimations)
        cancelAll(mAddAnimations)
        cancelAll(mChangeAnimations)

        dispatchAnimationsFinished()
    }

    private fun cancelAll(viewHolders: List<RecyclerView.ViewHolder>) {
        for (i in viewHolders.indices.reversed()) {
            val spring = viewHolders[i].itemView.getTag(R.id.spring) as? SpringAnimation
            spring?.cancel()
        }
    }

    private fun endChangeAnimation(infoList: MutableList<ChangeInfo>, item: RecyclerView.ViewHolder) {
        for (i in infoList.indices.reversed()) {
            val changeInfo = infoList[i]
            if (endChangeAnimationIfNecessary(changeInfo, item)) {
                if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
                    infoList.removeAt(i)
                }
            }
        }
    }

    private fun endChangeAnimationIfNecessary(changeInfo: ChangeInfo) {
        changeInfo.oldHolder?.let { endChangeAnimationIfNecessary(changeInfo, it) }
        changeInfo.newHolder?.let { endChangeAnimationIfNecessary(changeInfo, it) }
    }

    private fun endChangeAnimationIfNecessary(changeInfo: ChangeInfo, item: RecyclerView.ViewHolder): Boolean {
        var oldItem = false
        if (changeInfo.newHolder === item) {
            changeInfo.newHolder = null
        } else if (changeInfo.oldHolder === item) {
            changeInfo.oldHolder = null
            oldItem = true
        } else {
            return false
        }
        item.itemView.alpha = 1f
        item.itemView.translationX = 0f
        item.itemView.translationY = 0f
        dispatchChangeFinished(item, oldItem)
        return true
    }

    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder, payloads: MutableList<Any>): Boolean {
        return payloads.isNotEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads)
    }

    companion object {
        private val sDefaultInterpolator: TimeInterpolator = PathInterpolator(0.25f, 0.1f, 0.25f, 1f)
    }
}
